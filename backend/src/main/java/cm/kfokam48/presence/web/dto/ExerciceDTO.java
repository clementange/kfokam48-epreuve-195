package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.domaine.Exercice;
import cm.kfokam48.presence.domaine.StatutExercice;

import java.time.Instant;

/** Vue complète d'un exercice, renvoyée après un remplacement de lien. */
public record ExerciceDTO(
        Long id,
        Long sessionId,
        Long etudiantId,
        String lien,
        StatutExercice statut,
        Instant deposeAt,
        Instant majAt
) {
    public static ExerciceDTO de(Exercice e) {
        return new ExerciceDTO(e.getId(), e.getSessionId(), e.getEtudiantId(),
                e.getLien(), e.getStatut(), e.getDeposeAt(), e.getMajAt());
    }
}
