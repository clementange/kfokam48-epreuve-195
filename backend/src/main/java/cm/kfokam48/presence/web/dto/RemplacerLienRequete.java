package cm.kfokam48.presence.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Corps de {@code PUT /api/exercices/{id}} — opération ajoutée au contrat (Q13). */
public record RemplacerLienRequete(

        @NotBlank(message = "le lien de l'exercice est obligatoire")
        String lien
) {
}
