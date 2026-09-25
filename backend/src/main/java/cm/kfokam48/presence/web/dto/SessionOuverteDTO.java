package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.domaine.Session;

import java.time.Instant;

/**
 * Réponse {@code 201} de {@code POST /api/sessions}.
 *
 * <p>Le contrat impose exactement ces quatre champs. Aucune entité JPA n'est
 * exposée en JSON (B3) : ce record est la frontière.
 */
public record SessionOuverteDTO(
        Long id,
        String code,
        Instant ouvertureAt,
        Instant expirationAt
) {
    public static SessionOuverteDTO de(Session session) {
        return new SessionOuverteDTO(
                session.getId(),
                session.getCode(),
                session.getOuvertureAt(),
                session.getExpirationAt()
        );
    }
}
