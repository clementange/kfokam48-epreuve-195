package cm.kfokam48.presence.domaine;

import jakarta.persistence.*;

/**
 * Le formateur responsable d'une promotion.
 *
 * <p>Il n'est pas authentifié : Q1 supprime les mots de passe et ne dit rien de
 * l'identification du formateur. Cette entité existe pour que le tableau de bord
 * puisse afficher un nom, pas pour contrôler un accès — faiblesse assumée et
 * documentée au §7 du cahier des charges.
 */
@Entity
@Table(name = "formateur")
public class Formateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    protected Formateur() {
        // requis par JPA
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }
}
