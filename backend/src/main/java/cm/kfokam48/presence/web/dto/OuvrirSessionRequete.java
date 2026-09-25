package cm.kfokam48.presence.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/sessions}, tel que le contrat l'impose :
 * {@code { titre, promotionId }}, les deux obligatoires.
 */
public record OuvrirSessionRequete(

        @NotBlank(message = "le titre de la séance est obligatoire")
        @Size(max = 200, message = "le titre ne peut pas dépasser 200 caractères")
        String titre,

        @NotNull(message = "la promotion est obligatoire")
        Long promotionId
) {
}
