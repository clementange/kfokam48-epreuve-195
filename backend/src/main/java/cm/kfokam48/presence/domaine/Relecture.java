package cm.kfokam48.presence.domaine;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * La relecture d'un exercice par un pair.
 *
 * <p>Elle est <b>créée vide à la clôture</b> de la séance (RG14), au statut
 * {@code EN_ATTENTE}. C'est son identifiant que {@code POST /api/relectures/{id}}
 * attend : l'opération du contrat <em>renseigne</em> une relecture existante,
 * elle ne la crée pas.
 */
@Entity
@Table(name = "relecture")
public class Relecture {

    /** RG18 — la note est un entier de 0 à 20 (Q9). */
    public static final int NOTE_MINIMALE = 0;
    public static final int NOTE_MAXIMALE = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercice_id", nullable = false, unique = true)
    private Long exerciceId;

    @Column(name = "relecteur_id", nullable = false)
    private Long relecteurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutRelecture statut;

    private Integer note;

    @Column(length = 2000)
    private String commentaire;

    @Column(name = "assignee_at", nullable = false)
    private Instant assigneeAt;

    @Column(name = "rendue_at")
    private Instant rendueAt;

    protected Relecture() {
        // requis par JPA
    }

    private Relecture(Long exerciceId, Long relecteurId, Instant assigneeAt) {
        this.exerciceId = exerciceId;
        this.relecteurId = relecteurId;
        this.statut = StatutRelecture.EN_ATTENTE;
        this.assigneeAt = assigneeAt;
    }

    /** RG14, RG15 — assignation au moment de la clôture. */
    public static Relecture assigner(Long exerciceId, Long relecteurId, Instant maintenant) {
        return new Relecture(exerciceId, relecteurId, maintenant);
    }

    public boolean estRendue() {
        return statut == StatutRelecture.RENDUE;
    }

    /**
     * RG18, RG19 — enregistre la note. L'appelant doit avoir vérifié que la
     * relecture n'est pas déjà rendue : une note validée est définitive.
     */
    public void rendre(int note, String commentaire, Instant maintenant) {
        this.note = note;
        this.commentaire = commentaire;
        this.statut = StatutRelecture.RENDUE;
        this.rendueAt = maintenant;
    }

    /** RG18 — vrai si la note est un entier recevable. */
    public static boolean noteRecevable(Integer note) {
        return note != null && note >= NOTE_MINIMALE && note <= NOTE_MAXIMALE;
    }

    /**
     * RG18, Q9 — « Sur 20, en nombres entiers. »
     *
     * <p>Une note décimale est refusée et non arrondie : arrondir déciderait à la
     * place du relecteur, et l'étudiant recevrait une note que personne n'a mise.
     * {@code 16.0} est en revanche accepté — c'est bien un entier, écrit autrement.
     */
    public static boolean noteRecevable(BigDecimal note) {
        if (note == null || note.stripTrailingZeros().scale() > 0) {
            return false;
        }
        return noteRecevable(note.intValueExact());
    }

    public Long getId() {
        return id;
    }

    public Long getExerciceId() {
        return exerciceId;
    }

    public Long getRelecteurId() {
        return relecteurId;
    }

    public StatutRelecture getStatut() {
        return statut;
    }

    public Integer getNote() {
        return note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public Instant getAssigneeAt() {
        return assigneeAt;
    }

    public Instant getRendueAt() {
        return rendueAt;
    }
}
