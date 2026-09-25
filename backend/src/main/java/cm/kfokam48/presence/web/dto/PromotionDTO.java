package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.domaine.Promotion;

/** Une promotion, telle que les sélecteurs des trois écrans l'affichent. */
public record PromotionDTO(Long id, String nom, Long formateurId, String formateurNom) {

    public static PromotionDTO de(Promotion p, String formateurNom) {
        return new PromotionDTO(p.getId(), p.getNom(), p.getFormateurId(), formateurNom);
    }
}
