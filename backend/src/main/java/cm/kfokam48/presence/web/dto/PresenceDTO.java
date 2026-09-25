package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.domaine.Presence;
import cm.kfokam48.presence.domaine.SourcePresence;

/**
 * Réponse {@code 201} de {@code POST /api/presences}.
 *
 * <p>Les quatre champs du contrat, ni plus ni moins. {@code source} vaut
 * {@code ETUDIANT} ou {@code FORMATEUR} : c'est la trace exigée par Q14.
 */
public record PresenceDTO(Long id, Long sessionId, Long etudiantId, SourcePresence source) {

    public static PresenceDTO de(Presence p) {
        return new PresenceDTO(p.getId(), p.getSessionId(), p.getEtudiantId(), p.getSource());
    }
}
