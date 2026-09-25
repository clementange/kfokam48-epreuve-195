package cm.kfokam48.presence.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Corps de {@code POST /api/relectures/{id}}, imposé par le contrat.
 *
 * <p>La note est reçue en {@link BigDecimal} et non en {@code Integer}, pour une
 * raison de fond : Jackson accepte {@code 15.5} pour un {@code Integer} et le
 * <b>tronque silencieusement en 15</b>. Le relecteur croirait avoir mis 15,5 et
 * l'étudiant recevrait 15, sans que personne ne soit averti. Q9 est explicite —
 * « Sur 20, en nombres entiers » — et le contrat exige {@code 400} pour une note
 * non entière : le caractère entier doit donc être vérifié, pas subi.
 *
 * <p>La borne 0–20 n'est volontairement pas exprimée par {@code @Min}/{@code @Max} :
 * une violation de validation produirait {@code REQUETE_INVALIDE}, alors que le
 * contrat impose {@code NOTE_INVALIDE}. C'est le service qui tranche (RG18).
 */
public record RendreRelectureRequete(

        @NotNull(message = "la note est obligatoire")
        BigDecimal note,

        @NotNull(message = "le commentaire est obligatoire")
        String commentaire
) {
}
