package cm.kfokam48.presence.service;

import cm.kfokam48.presence.commun.erreur.CodeErreur;
import cm.kfokam48.presence.commun.erreur.ErreurMetier;
import cm.kfokam48.presence.domaine.Etudiant;
import cm.kfokam48.presence.domaine.Presence;
import cm.kfokam48.presence.domaine.Session;
import cm.kfokam48.presence.repository.PresenceRepository;
import cm.kfokam48.presence.repository.SessionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * EF2 — le marquage de présence, et les six façons dont il peut échouer.
 *
 * <p><b>L'ordre des contrôles est signifiant</b> et suit le diagramme D3. En
 * particulier, la clôture est vérifiée <em>avant</em> l'expiration : une séance
 * clôturée le reste définitivement, alors qu'un code expiré sur une séance
 * encore ouverte décrit une situation différente — le formateur peut encore
 * ajouter la présence à la main (RG8). Dire « expiré » dans les deux cas
 * tromperait l'étudiant sur ce qu'il peut faire ensuite.
 */
@Service
public class PresenceService {

    /**
     * Nom de la contrainte posée par la migration V1. Il est écrit ici parce que
     * c'est le seul moyen de distinguer « déjà présent » d'une autre violation
     * d'intégrité : le pilote JDBC ne renvoie qu'un message. S'il change en base,
     * il doit changer ici — d'où le test qui le vérifie.
     */
    static final String CONTRAINTE_UNICITE_PRESENCE = "uq_presence_session_etudiant";

    private final SessionRepository sessions;
    private final PresenceRepository presences;
    private final ReferentielService referentiel;
    private final Clock horloge;

    public PresenceService(SessionRepository sessions,
                           PresenceRepository presences,
                           ReferentielService referentiel,
                           Clock horloge) {
        this.sessions = sessions;
        this.presences = presences;
        this.referentiel = referentiel;
        this.horloge = horloge;
    }

    /**
     * EF2 — l'étudiant saisit le code et sa présence est enregistrée.
     *
     * @throws ErreurMetier {@code ETUDIANT_INCONNU}, {@code CODE_INCONNU},
     *                      {@code SESSION_CLOTUREE}, {@code CODE_EXPIRE} ou
     *                      {@code DEJA_PRESENT}
     */
    @Transactional
    public Presence marquer(String code, Long etudiantId) {
        Etudiant etudiant = referentiel.exigerEtudiantExistant(etudiantId);

        // RG4, RG24 — le code n'est cherché que dans la promotion de l'étudiant.
        // Un code valide appartenant à une autre promotion reste « inconnu » :
        // répondre « ce code n'est pas pour vous » confirmerait son existence,
        // ce que Q4 cherche précisément à éviter.
        Session seance = sessions
                .findByCodeAndPromotionId(normaliser(code), etudiant.getPromotionId())
                .orElseThrow(() -> new ErreurMetier(CodeErreur.CODE_INCONNU));

        Instant maintenant = horloge.instant();
        exigerSeanceRecevable(seance, maintenant);

        if (presences.existsBySessionIdAndEtudiantId(seance.getId(), etudiantId)) {
            throw new ErreurMetier(CodeErreur.DEJA_PRESENT);
        }

        return enregistrer(Presence.marqueeParEtudiant(seance.getId(), etudiantId, maintenant));
    }

    /**
     * EF9, RG8 — le formateur ajoute une présence à la main.
     *
     * <p>Q14 : « ça arrive qu'un étudiant ait un souci de téléphone. Mais il faut
     * que ça se voie : marquez "ajouté par le formateur". » D'où la source
     * {@code FORMATEUR}, qui est visible jusque sur l'écran.
     *
     * <p><b>L'expiration du code ne s'applique pas ici</b>, et c'est délibéré :
     * cette opération est le recours prévu quand le code ne marche plus. La
     * refuser au motif que le code a expiré la viderait de son sens. La clôture,
     * elle, reste bloquante — une séance close ne reçoit plus rien (RG6).
     */
    @Transactional
    public Presence ajouterParFormateur(Long sessionId, Long etudiantId) {
        Etudiant etudiant = referentiel.exigerEtudiantExistant(etudiantId);

        Session seance = sessions.findById(sessionId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.SESSION_INCONNUE));

        // RG24 — le formateur ne peut pas inscrire un étudiant d'une autre
        // promotion. Ici le message est explicite, contrairement au marquage par
        // code : le formateur a le droit de savoir pourquoi c'est refusé, il
        // n'est pas en train de deviner un code.
        if (!etudiant.appartientA(seance.getPromotionId())) {
            throw new ErreurMetier(CodeErreur.ETUDIANT_HORS_PROMOTION);
        }

        if (seance.estCloturee()) {
            throw new ErreurMetier(CodeErreur.SESSION_CLOTUREE);
        }

        if (presences.existsBySessionIdAndEtudiantId(sessionId, etudiantId)) {
            throw new ErreurMetier(CodeErreur.DEJA_PRESENT);
        }

        return enregistrer(Presence.ajouteeParFormateur(sessionId, etudiantId, horloge.instant()));
    }

    /** RG6 puis RG1/RG5 — l'ordre compte, voir la documentation de la classe. */
    private void exigerSeanceRecevable(Session seance, Instant maintenant) {
        if (seance.estCloturee()) {
            throw new ErreurMetier(CodeErreur.SESSION_CLOTUREE);
        }
        if (seance.codeExpire(maintenant)) {
            throw new ErreurMetier(CodeErreur.CODE_EXPIRE);
        }
    }

    /**
     * RG3 — le {@code existsBy} plus haut couvre le cas courant, mais deux
     * requêtes simultanées le franchiraient toutes les deux. Seule la contrainte
     * d'unicité en base tranche, et sa violation est traduite ici dans le code
     * d'erreur du contrat plutôt que de remonter en 500.
     *
     * <p><b>Issue #31</b> — seule la violation de {@code uq_presence_session_etudiant}
     * signifie « déjà présent ». Auparavant ce bloc attrapait <em>toute</em>
     * violation d'intégrité : une clé étrangère cassée ou un {@code CHECK} violé
     * répondaient « Votre présence est déjà enregistrée » à un étudiant qui ne
     * l'était pas. Cela envoyait l'utilisateur chercher un problème inexistant
     * et masquait au passage un défaut d'intégrité réel, que plus personne ne
     * voyait puisqu'il ressortait en message métier rassurant.
     *
     * <p>Les autres violations sont relancées : elles finissent en
     * {@code 500 ERREUR_INTERNE} par le {@link cm.kfokam48.presence.commun.web.GestionnaireErreurs},
     * avec la trace complète côté serveur. Une panne doit se voir comme une
     * panne.
     */
    private Presence enregistrer(Presence presence) {
        try {
            return presences.saveAndFlush(presence);
        } catch (DataIntegrityViolationException e) {
            if (violeLUniciteDeLaPresence(e)) {
                throw new ErreurMetier(CodeErreur.DEJA_PRESENT);
            }
            throw e;
        }
    }

    /**
     * Cherche le nom de la contrainte dans toute la chaîne des causes.
     *
     * <p>Le nom n'est pas toujours dans le message de surface : Hibernate et le
     * pilote JDBC l'enveloppent. On parcourt donc les causes jusqu'à la racine.
     * La comparaison est insensible à la casse, PostgreSQL et H2 ne formatant
     * pas leurs messages de la même façon.
     */
    private boolean violeLUniciteDeLaPresence(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null
                    && message.toLowerCase().contains(CONTRAINTE_UNICITE_PRESENCE)) {
                return true;
            }
        }
        return false;
    }

    /** Le code est saisi à la main, souvent recopié d'un tableau : on tolère la casse et les espaces. */
    private String normaliser(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
