package cm.kfokam48.presence.domaine;

import jakarta.persistence.*;

/**
 * Une promotion regroupe les étudiants d'une formation et les séances qui leur
 * sont destinées.
 *
 * <p>Les clés étrangères sont portées par de simples identifiants plutôt que par
 * des associations JPA. Le modèle est petit, les DTO sont obligatoires (B3) et
 * aucune navigation d'objet n'est nécessaire : une relation gérée coûterait des
 * chargements paresseux sans rien apporter.
 */
@Entity
@Table(name = "promotion")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(name = "formateur_id", nullable = false)
    private Long formateurId;

    protected Promotion() {
        // requis par JPA
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public Long getFormateurId() {
        return formateurId;
    }
}
