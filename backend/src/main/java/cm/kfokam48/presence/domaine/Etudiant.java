package cm.kfokam48.presence.domaine;

import jakarta.persistence.*;

/**
 * Un étudiant, rattaché à une seule promotion (RG24).
 *
 * <p>Il n'y a pas d'entité « relecteur » : le rôle est temporaire et porté par
 * la relecture assignée. Un même étudiant est simultanément auteur d'un exercice
 * et relecteur d'un autre (cahier des charges §2).
 */
@Entity
@Table(name = "etudiant")
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    protected Etudiant() {
        // requis par JPA
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    /** RG24 — un étudiant n'agit que dans le cadre de sa propre promotion. */
    public boolean appartientA(Long promotion) {
        return promotionId.equals(promotion);
    }
}
