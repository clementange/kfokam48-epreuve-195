package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.domaine.Etudiant;

/**
 * Un étudiant dans la liste où il se désigne (Q1, RG25).
 *
 * <p>Cette liste tient lieu d'identification : il n'y a pas d'authentification.
 */
public record EtudiantDTO(Long id, String nom, Long promotionId) {

    public static EtudiantDTO de(Etudiant e) {
        return new EtudiantDTO(e.getId(), e.getNom(), e.getPromotionId());
    }
}
