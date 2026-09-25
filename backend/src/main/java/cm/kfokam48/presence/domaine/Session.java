package cm.kfokam48.presence.domaine;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Une séance de cours ouverte par le formateur.
 *
 * <p>Deux notions à ne pas confondre, et que la demande du client mélangeait
 * (cahier des charges §7) :
 * <ul>
 *   <li>l'<b>expiration du code</b> — 15 minutes après l'ouverture (RG1). Passé
 *       ce délai le code ne marche plus, mais la séance reste ouverte ;</li>
 *   <li>la <b>clôture</b> — décidée par le formateur, irréversible (RG14). Elle
 *       ferme le dépôt d'exercices et déclenche l'assignation des relecteurs.</li>
 * </ul>
 */
@Entity
@Table(name = "session")
public class Session {

    /** RG1 — durée de validité du code, à compter de l'ouverture. */
    public static final int MINUTES_DE_VALIDITE_DU_CODE = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @Column(name = "ouverture_at", nullable = false)
    private Instant ouvertureAt;

    @Column(name = "expiration_at", nullable = false)
    private Instant expirationAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSession statut;

    @Column(name = "cloture_at")
    private Instant clotureAt;

    protected Session() {
        // requis par JPA
    }

    private Session(String titre, Long promotionId, String code, Instant ouvertureAt) {
        this.titre = titre;
        this.promotionId = promotionId;
        this.code = code;
        this.ouvertureAt = ouvertureAt;
        this.expirationAt = ouvertureAt.plusSeconds(MINUTES_DE_VALIDITE_DU_CODE * 60L);
        this.statut = StatutSession.OUVERTE;
    }

    /**
     * Ouvre une séance. L'expiration est calculée ici et nulle part ailleurs :
     * RG1 n'a qu'un seul endroit où être vraie.
     */
    public static Session ouvrir(String titre, Long promotionId, String code, Instant maintenant) {
        return new Session(titre, promotionId, code, maintenant);
    }

    /** RG1, RG5 — le code ne vaut plus rien passé l'expiration. */
    public boolean codeExpire(Instant maintenant) {
        return !maintenant.isBefore(expirationAt);
    }

    /** RG6, RG12 — une séance clôturée n'accepte plus ni présence ni dépôt. */
    public boolean estCloturee() {
        return statut == StatutSession.CLOTUREE;
    }

    /** RG14 — irréversible. Appeler deux fois est une erreur métier, pas un cas normal. */
    public void cloturer(Instant maintenant) {
        this.statut = StatutSession.CLOTUREE;
        this.clotureAt = maintenant;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public String getCode() {
        return code;
    }

    public Instant getOuvertureAt() {
        return ouvertureAt;
    }

    public Instant getExpirationAt() {
        return expirationAt;
    }

    public StatutSession getStatut() {
        return statut;
    }

    public Instant getClotureAt() {
        return clotureAt;
    }
}
