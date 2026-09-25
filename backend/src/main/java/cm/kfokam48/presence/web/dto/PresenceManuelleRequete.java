package cm.kfokam48.presence.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/presences/manuelles} — opération ajoutée au contrat.
 *
 * <p>Aucun code n'est demandé : c'est précisément le recours prévu quand le code
 * ne fonctionne pas pour l'étudiant (Q14).
 */
public record PresenceManuelleRequete(

        @NotNull(message = "la séance est obligatoire")
        Long sessionId,

        @NotNull(message = "l'étudiant est obligatoire")
        Long etudiantId
) {
}
