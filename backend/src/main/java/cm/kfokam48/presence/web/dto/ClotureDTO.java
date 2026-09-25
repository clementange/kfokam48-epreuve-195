package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.domaine.StatutSession;
import cm.kfokam48.presence.service.ResultatCloture;

import java.time.Instant;

/**
 * Réponse {@code 200} de {@code POST /api/sessions/{id}/cloture}.
 *
 * <p>{@code exercicesNonAssignes} rend RG16 visible : le formateur apprend, au
 * moment où il clôture, qu'un exercice ne sera jamais noté faute de relecteur
 * éligible.
 */
public record ClotureDTO(
        Long id,
        StatutSession statut,
        Instant clotureAt,
        int relecturesAssignees,
        int exercicesNonAssignes
) {
    public static ClotureDTO de(ResultatCloture r) {
        return new ClotureDTO(
                r.seance().getId(),
                r.seance().getStatut(),
                r.seance().getClotureAt(),
                r.relecturesAssignees(),
                r.exercicesNonAssignes());
    }
}
