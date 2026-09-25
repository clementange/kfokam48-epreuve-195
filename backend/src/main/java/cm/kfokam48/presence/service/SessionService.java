package cm.kfokam48.presence.service;

import cm.kfokam48.presence.commun.erreur.CodeErreur;
import cm.kfokam48.presence.commun.erreur.ErreurMetier;
import cm.kfokam48.presence.domaine.Session;
import cm.kfokam48.presence.repository.PromotionRepository;
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
    private final GenerateurCode generateurCode;
    private final Clock horloge;

    public SessionService(SessionRepository sessions,
                          PromotionRepository promotions,
                          GenerateurCode generateurCode,
                          Clock horloge) {
        this.sessions = sessions;
        this.promotions = promotions;
        this.generateurCode = generateurCode;
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
