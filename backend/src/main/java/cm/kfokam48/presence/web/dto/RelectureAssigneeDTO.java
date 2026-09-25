package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.domaine.StatutRelecture;

/**
 * Ce que voit le relecteur sur son écran.
 *
 * <p>{@code relectureId} est l'identifiant attendu par
 * {@code POST /api/relectures/{id}} : sans cette opération, aucun client ne
 * pourrait le connaître.
 *
 * <p>Le nom de l'auteur est renvoyé : l'anonymat de Q8 protège le relecteur,
 * pas l'auteur.
 */
public record RelectureAssigneeDTO(
        Long relectureId,
        Long exerciceId,
        Long sessionId,
        String sessionTitre,
        String auteurNom,
        String lien,
        StatutRelecture statut,
        Integer note,
        String commentaire
) {
}
