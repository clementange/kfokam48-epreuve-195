package cm.kfokam48.presence.domaine;

/**
 * Cycle de vie d'un exercice — diagramme D4.
 *
 * <pre>
 *   DEPOSE ──(clôture, relecteur tiré)──▶ EN_ATTENTE_RELECTURE ──(note)──▶ RELU
 *      └────(clôture, aucun éligible)───▶ NON_ASSIGNE
 * </pre>
 */
public enum StatutExercice {

    /** Déposé, la séance est encore ouverte : le lien reste remplaçable (RG13). */
    DEPOSE,

    /** Un relecteur est assigné, il n'a pas encore rendu (RG21, Q11). */
    EN_ATTENTE_RELECTURE,

    /** Noté. La note est définitive (RG19). */
    RELU,

    /**
     * RG16 — aucun relecteur éligible n'existait à la clôture : l'auteur était
     * le seul présent, ou le seul à avoir déposé. L'exercice n'entre dans
     * aucune moyenne et le formateur le voit comme tel.
     */
    NON_ASSIGNE
}
