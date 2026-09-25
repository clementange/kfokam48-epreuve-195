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
     */
    private Presence enregistrer(Presence presence) {
        try {
            return presences.saveAndFlush(presence);
        } catch (DataIntegrityViolationException e) {
            throw new ErreurMetier(CodeErreur.DEJA_PRESENT);
        }
    }

    /** Le code est saisi à la main, souvent recopié d'un tableau : on tolère la casse et les espaces. */
    private String normaliser(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
