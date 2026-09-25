package cm.kfokam48.presence.service;

import cm.kfokam48.presence.commun.erreur.CodeErreur;
import cm.kfokam48.presence.commun.erreur.ErreurMetier;
import cm.kfokam48.presence.domaine.Exercice;
import cm.kfokam48.presence.domaine.Session;
import cm.kfokam48.presence.repository.ExerciceRepository;
import cm.kfokam48.presence.repository.PresenceRepository;
import cm.kfokam48.presence.repository.SessionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * EF4, EF10 — le dépôt et le remplacement du lien d'un exercice.
 *
 * <p>Point important, et c'est une décision documentée au §7 du cahier des
 * charges : <b>l'expiration du code ne ferme pas le dépôt</b>. Q12 est explicite
 * — « jusqu'à ce que je clôture la session, certains n'ont pas de connexion le
 * soir même ». Seule la clôture ferme le dépôt (RG12).
 */
@Service
public class ExerciceService {

    private final ExerciceRepository exercices;
    private final SessionRepository sessions;
    private final PresenceRepository presences;
    private final ReferentielService referentiel;
    private final Clock horloge;

    public ExerciceService(ExerciceRepository exercices,
                           SessionRepository sessions,
                           PresenceRepository presences,
                           ReferentielService referentiel,
                           Clock horloge) {
        this.exercices = exercices;
        this.sessions = sessions;
        this.presences = presences;
        this.referentiel = referentiel;
        this.horloge = horloge;
    }

    /** EF4 — l'étudiant dépose le lien de son exercice. */
    @Transactional
    public Exercice deposer(Long sessionId, Long etudiantId, String lien) {
        referentiel.exigerEtudiantExistant(etudiantId);

        if (!ValidateurLien.estValide(lien)) {
            throw new ErreurMetier(CodeErreur.LIEN_INVALIDE);
        }

        Session seance = sessions.findById(sessionId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.SESSION_INCONNUE));

        // RG12 — le dépôt reste ouvert après l'expiration du code, jusqu'à la
        // clôture. C'est la distinction expiration / clôture qui rend Q12
        // applicable.
        if (seance.estCloturee()) {
            throw new ErreurMetier(CodeErreur.SESSION_CLOTUREE);
        }

        // RG11 — un absent n'est pas relisible par ses pairs, puisque le
        // relecteur est tiré parmi les présents (Q7).
        if (!presences.existsBySessionIdAndEtudiantId(sessionId, etudiantId)) {
            throw new ErreurMetier(CodeErreur.ETUDIANT_ABSENT);
        }

        if (exercices.existsBySessionIdAndEtudiantId(sessionId, etudiantId)) {
            throw new ErreurMetier(CodeErreur.EXERCICE_DEJA_DEPOSE);
        }

        try {
            return exercices.saveAndFlush(
                    Exercice.deposer(sessionId, etudiantId, lien.trim(), horloge.instant()));
        } catch (DataIntegrityViolationException e) {
            // RG9 — deux dépôts simultanés : la contrainte d'unicité tranche.
            throw new ErreurMetier(CodeErreur.EXERCICE_DEJA_DEPOSE);
        }
    }

    /**
     * EF10, RG13 — remplacement du lien.
     *
     * <p>Q13 autorise le remplacement « tant que personne n'a commencé à le
     * relire ». Aucun événement observable ne correspond à « commencé » : on
     * l'assimile à « avant assignation », c'est-à-dire tant que la séance est
     * ouverte (cahier des charges §7).
     */
    @Transactional
    public Exercice remplacerLien(Long exerciceId, String nouveauLien) {
        if (!ValidateurLien.estValide(nouveauLien)) {
            throw new ErreurMetier(CodeErreur.LIEN_INVALIDE);
        }

        Exercice exercice = exercices.findById(exerciceId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.EXERCICE_INCONNU));

        Session seance = sessions.findById(exercice.getSessionId())
                .orElseThrow(() -> new ErreurMetier(CodeErreur.SESSION_INCONNUE));

        if (seance.estCloturee()) {
            throw new ErreurMetier(CodeErreur.SESSION_CLOTUREE);
        }

        exercice.remplacerLien(nouveauLien.trim(), horloge.instant());
        return exercices.save(exercice);
    }
}
