package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.domaine.StatutExercice;

import java.math.BigDecimal;

/**
 * Ce que voit l'auteur d'un exercice (EF11, RG20).
 *
 * <p>Q8 — « Oui, la note et le commentaire. Mais pas le nom du relecteur. »
 * Aucun champ ne permet, directement ou par recoupement, d'identifier un
 * relecteur : les commentaires des deux relecteurs sont présentés ensemble,
 * sans dire qui a écrit quoi.
 *
 * <p>Issue #35 — {@code note} est la <b>moyenne</b> des relectures rendues.
 * {@code provisoire} dit qu'une relecture assignée n'a pas encore été rendue et
 * que la moyenne peut encore changer. {@code relecturesRendues} et
 * {@code relecturesAttendues} rendent cette situation lisible sans avoir à
 * l'interpréter.
 */
public record MonExerciceDTO(
        Long exerciceId,
        Long sessionId,
        String sessionTitre,
        String lien,
        StatutExercice statut,
        BigDecimal note,
        String commentaire,
        boolean provisoire,
        int relecturesRendues,
        int relecturesAttendues
) {
}
