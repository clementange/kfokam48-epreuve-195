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
 *
 * <p>{@code exercicesUnSeulRelecteur} vient de l'issue #34 : un exercice n'ayant
 * trouvé qu'un pair éligible recevra une note, mais pas une moyenne. Le
 * distinguer évite que le formateur prenne cette note pour le résultat de deux
 * relectures concordantes.
 */
public record ClotureDTO(
        Long id,
        StatutSession statut,
        Instant clotureAt,
        int relecturesAssignees,
        int exercicesNonAssignes,
        int exercicesUnSeulRelecteur
) {
    public static ClotureDTO de(ResultatCloture r) {
        return new ClotureDTO(
                r.seance().getId(),
                r.seance().getStatut(),
                r.seance().getClotureAt(),
                r.relecturesAssignees(),
                r.exercicesNonAssignes(),
                r.exercicesUnSeulRelecteur());
    }
}
