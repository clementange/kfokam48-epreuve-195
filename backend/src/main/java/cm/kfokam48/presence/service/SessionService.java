package cm.kfokam48.presence.service;

import cm.kfokam48.presence.commun.erreur.CodeErreur;
import cm.kfokam48.presence.commun.erreur.ErreurMetier;
import cm.kfokam48.presence.domaine.Exercice;
import cm.kfokam48.presence.domaine.Presence;
import cm.kfokam48.presence.domaine.Relecture;
import cm.kfokam48.presence.domaine.Session;
import cm.kfokam48.presence.repository.ExerciceRepository;
import cm.kfokam48.presence.repository.PresenceRepository;
import cm.kfokam48.presence.repository.PromotionRepository;
import cm.kfokam48.presence.repository.RelectureRepository;
import cm.kfokam48.presence.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Les règles de gestion des séances.
 *
 * <p>Toute la logique vit ici : le contrôleur n'appelle jamais un repository,
 * et aucune requête base ne traverse la couche web (B3).
 */
@Service
public class SessionService {

    /**
     * RG2 — le code est unique. La collision est improbable (32^6 valeurs) mais
     * pas impossible : on retire plutôt que de laisser la contrainte d'unicité
     * faire échouer l'ouverture d'une séance au visage du formateur.
     */
    private static final int TIRAGES_MAXIMUM = 10;

    private final SessionRepository sessions;
    private final PromotionRepository promotions;
    private final PresenceRepository presences;
    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;
    private final GenerateurCode generateurCode;
    private final TirageRelecteur tirage;
    private final Clock horloge;

    public SessionService(SessionRepository sessions,
                          PromotionRepository promotions,
                          PresenceRepository presences,
                          ExerciceRepository exercices,
                          RelectureRepository relectures,
                          GenerateurCode generateurCode,
                          TirageRelecteur tirage,
                          Clock horloge) {
        this.sessions = sessions;
        this.promotions = promotions;
        this.presences = presences;
        this.exercices = exercices;
        this.relectures = relectures;
        this.generateurCode = generateurCode;
        this.tirage = tirage;
        this.horloge = horloge;
    }

    /** EF1 — le formateur ouvre une séance et obtient son code. */
    @Transactional
    public Session ouvrir(String titre, Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw new ErreurMetier(CodeErreur.PROMOTION_INCONNUE);
        }
        Instant maintenant = horloge.instant();
        return sessions.save(Session.ouvrir(titre, promotionId, tirerUnCodeLibre(), maintenant));
    }

    @Transactional(readOnly = true)
    public List<Session> listerParPromotion(Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw new ErreurMetier(CodeErreur.PROMOTION_INCONNUE);
        }
        return sessions.findByPromotionIdOrderByOuvertureAtDesc(promotionId);
    }

    /**
     * EF5, EF6 — la clôture, et l'assignation qu'elle déclenche.
     *
     * <p><b>Clôturer, c'est assigner.</b> Le client n'a jamais dit quand le
     * relecteur devait être désigné (Q7). La décision, prise au §7 du cahier des
     * charges, est que ce soit à la clôture : c'est le seul instant où la liste
     * des présents et la liste des exercices sont toutes deux définitives. Cette
     * seule décision rend Q7, Q12 et Q13 compatibles entre elles.
     *
     * <p>RG14 — la clôture est irréversible. Aucune opération ne rouvre une séance.
     */
    @Transactional
    public ResultatCloture cloturer(Long sessionId) {
        Session seance = sessions.findById(sessionId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.SESSION_INCONNUE));

        if (seance.estCloturee()) {
            throw new ErreurMetier(CodeErreur.SESSION_DEJA_CLOTUREE);
        }

        Instant maintenant = horloge.instant();
        seance.cloturer(maintenant);
        sessions.save(seance);

        List<Long> presents = presences.findBySessionIdOrderByEtudiantIdAsc(sessionId)
                .stream().map(Presence::getEtudiantId).toList();

        int assignees = 0;
        int nonAssignes = 0;

        for (Exercice exercice : exercices.findBySessionId(sessionId)) {
            // RG17, Q5 — « Jamais. C'est le principe même. » L'auteur est retiré
            // des candidats plutôt que rejeté après tirage : la règle est ainsi
            // vraie par construction, pas par vérification.
            List<Long> candidats = presents.stream()
                    .filter(etudiantId -> !etudiantId.equals(exercice.getEtudiantId()))
                    .toList();

            Long relecteur = tirage.tirer(candidats);

            if (relecteur == null) {
                // RG16 — aucun éligible : l'auteur était seul présent, ou seul à
                // avoir déposé. Le client n'avait pas prévu ce cas ; il se produit
                // à chaque séance à un seul étudiant.
                exercice.marquerNonAssigne();
                nonAssignes++;
            } else {
                relectures.save(Relecture.assigner(exercice.getId(), relecteur, maintenant));
                exercice.marquerEnAttenteDeRelecture();
                assignees++;
            }
            exercices.save(exercice);
        }

        return new ResultatCloture(seance, assignees, nonAssignes);
    }

    private String tirerUnCodeLibre() {
        for (int essai = 0; essai < TIRAGES_MAXIMUM; essai++) {
            String code = generateurCode.genererCode();
            if (!sessions.existsByCode(code)) {
                return code;
            }
        }
        // Dix collisions d'affilée sur un milliard de valeurs : ce n'est plus de
        // la malchance, c'est un générateur cassé. Mieux vaut le dire.
        throw new IllegalStateException(
                "Impossible de tirer un code de présence libre après " + TIRAGES_MAXIMUM + " essais");
    }
}
