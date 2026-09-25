package cm.kfokam48.presence.domaine;

/**
 * Cycle de vie d'un exercice — diagramme D4.
 *
 * <pre>
 *   DEPOSE ──(clôture, 2 relecteurs)──▶ EN_ATTENTE_RELECTURE
 *                                            │
 *                                  (1ʳᵉ note) ▼
 *                                     PARTIELLEMENT_RELU ──(2ᵈᵉ note)──▶ RELU
 *      └───────(clôture, aucun éligible)──────────────────────────────▶ NON_ASSIGNE
 * </pre>
 *
 * <p>Révisé à l'étape 3 : le client demande deux relecteurs par exercice, d'où
 * l'état intermédiaire {@link #PARTIELLEMENT_RELU}.
 */
public enum StatutExercice {

    /** Déposé, la séance est encore ouverte : le lien reste remplaçable (RG13). */
    DEPOSE,

    /** Des relecteurs sont assignés, aucun n'a encore rendu (RG21, Q11). */
    EN_ATTENTE_RELECTURE,

    /**
     * Une relecture sur deux a été rendue. La note affichée est celle du seul
     * relecteur ayant répondu, et elle est <b>provisoire</b> : le second peut
     * encore rendre et la moyenne changera.
     */
    PARTIELLEMENT_RELU,

    /**
     * Toutes les relectures assignées ont été rendues. La note est définitive.
     *
     * <p>Un exercice qui n'avait qu'un seul relecteur éligible passe ici dès que
     * celui-ci a rendu : sa note n'est pas provisoire, il n'y a personne à
     * attendre.
     */
    RELU,

    /**
     * RG16 — aucun relecteur éligible n'existait à la clôture : l'auteur était
     * le seul présent, ou le seul à avoir déposé. L'exercice n'entre dans
     * aucune moyenne et le formateur le voit comme tel.
     */
    NON_ASSIGNE
}
