package cm.kfokam48.presence.domaine;

import jakarta.persistence.*;

import java.time.Instant;

/** L'exercice déposé par un étudiant pour une séance. */
@Entity
@Table(name = "exercice")
public class Exercice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "etudiant_id", nullable = false)
    private Long etudiantId;

    @Column(nullable = false, length = 500)
    private String lien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutExercice statut;

    @Column(name = "depose_at", nullable = false)
    private Instant deposeAt;

    @Column(name = "maj_at")
    private Instant majAt;

    protected Exercice() {
        // requis par JPA
    }

    private Exercice(Long sessionId, Long etudiantId, String lien, Instant maintenant) {
        this.sessionId = sessionId;
        this.etudiantId = etudiantId;
        this.lien = lien;
        this.statut = StatutExercice.DEPOSE;
        this.deposeAt = maintenant;
    }

    public static Exercice deposer(Long sessionId, Long etudiantId, String lien, Instant maintenant) {
        return new Exercice(sessionId, etudiantId, lien, maintenant);
    }

    /**
     * RG13 — le remplacement du lien ne touche pas {@code deposeAt} : la date de
     * dépôt reste celle du premier dépôt, {@code majAt} porte la dernière
     * modification. Écraser l'une par l'autre effacerait l'information utile.
     */
    public void remplacerLien(String nouveauLien, Instant maintenant) {
        this.lien = nouveauLien;
        this.majAt = maintenant;
    }

    /** RG14, RG15 — effet de la clôture lorsqu'un relecteur a pu être tiré. */
    public void marquerEnAttenteDeRelecture() {
        this.statut = StatutExercice.EN_ATTENTE_RELECTURE;
    }

    /** RG16 — effet de la clôture lorsqu'aucun relecteur n'était éligible. */
    public void marquerNonAssigne() {
        this.statut = StatutExercice.NON_ASSIGNE;
    }

    /** RG18 — la relecture a été rendue. */
    public void marquerRelu() {
        this.statut = StatutExercice.RELU;
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getEtudiantId() {
        return etudiantId;
    }

    public String getLien() {
        return lien;
    }

    public StatutExercice getStatut() {
        return statut;
    }

    public Instant getDeposeAt() {
        return deposeAt;
    }

    public Instant getMajAt() {
        return majAt;
    }
}
