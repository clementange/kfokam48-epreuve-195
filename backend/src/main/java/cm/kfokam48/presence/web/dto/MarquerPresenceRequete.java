package cm.kfokam48.presence.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps de {@code POST /api/presences}, imposé par le contrat. */
public record MarquerPresenceRequete(

        @NotBlank(message = "le code de présence est obligatoire")
        String code,

        @NotNull(message = "l'étudiant est obligatoire")
        Long etudiantId
) {
}
