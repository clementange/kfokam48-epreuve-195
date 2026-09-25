package cm.kfokam48.presence.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps de {@code POST /api/exercices}, imposé par le contrat. */
public record DeposerExerciceRequete(

        @NotNull(message = "la séance est obligatoire")
        Long sessionId,

        @NotNull(message = "l'étudiant est obligatoire")
        Long etudiantId,

        @NotBlank(message = "le lien de l'exercice est obligatoire")
        String lien
) {
}
