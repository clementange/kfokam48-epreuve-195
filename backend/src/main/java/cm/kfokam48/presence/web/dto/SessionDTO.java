package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.domaine.Session;
import cm.kfokam48.presence.domaine.StatutSession;

import java.time.Instant;

/** Vue complète d'une séance, pour l'écran formateur. */
public record SessionDTO(
        Long id,
        String titre,
        Long promotionId,
        String code,
        Instant ouvertureAt,
        Instant expirationAt,
        StatutSession statut,
        Instant clotureAt
) {
    public static SessionDTO de(Session s) {
        return new SessionDTO(
                s.getId(), s.getTitre(), s.getPromotionId(), s.getCode(),
                s.getOuvertureAt(), s.getExpirationAt(), s.getStatut(), s.getClotureAt()
        );
    }
}
