package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.domaine.StatutExercice;

/**
 * Ce que voit l'auteur d'un exercice (EF11, RG20).
 *
 * <p>Q8 — « Oui, la note et le commentaire. Mais pas le nom du relecteur. »
 * Aucun champ de ce record ne permet, directement ou par recoupement,
 * d'identifier le relecteur : ni son identifiant, ni son nom, ni la date à
 * laquelle il a rendu.
 */
public record MonExerciceDTO(
        Long exerciceId,
        Long sessionId,
        String sessionTitre,
        String lien,
        StatutExercice statut,
        Integer note,
        String commentaire
) {
}
