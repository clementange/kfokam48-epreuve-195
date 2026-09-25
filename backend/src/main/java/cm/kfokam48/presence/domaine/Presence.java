package cm.kfokam48.presence.domaine;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * La présence d'un étudiant à une séance.
 *
 * <p>RG3 — unique par couple (séance, étudiant). La contrainte est portée par la
 * base et pas seulement par le service : deux requêtes simultanées passeraient
 * toutes les deux la vérification applicative.
 */
@Entity
@Table(name = "presence")
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "etudiant_id", nullable = false)
    private Long etudiantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourcePresence source;

    @Column(name = "marquee_at", nullable = false)
    private Instant marqueeAt;

    protected Presence() {
        // requis par JPA
    }

    private Presence(Long sessionId, Long etudiantId, SourcePresence source, Instant marqueeAt) {
        this.sessionId = sessionId;
        this.etudiantId = etudiantId;
        this.source = source;
        this.marqueeAt = marqueeAt;
    }

    /** EF2 — l'étudiant a saisi le code lui-même. */
    public static Presence marqueeParEtudiant(Long sessionId, Long etudiantId, Instant maintenant) {
        return new Presence(sessionId, etudiantId, SourcePresence.ETUDIANT, maintenant);
    }

    /** EF9, RG8 — le formateur l'a ajoutée à la main (Q14). */
    public static Presence ajouteeParFormateur(Long sessionId, Long etudiantId, Instant maintenant) {
        return new Presence(sessionId, etudiantId, SourcePresence.FORMATEUR, maintenant);
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

    public SourcePresence getSource() {
        return source;
    }

    public Instant getMarqueeAt() {
        return marqueeAt;
    }
}
