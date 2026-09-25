package cm.kfokam48.presence.repository;

/**
 * Projection d'une ligne du tableau du formateur.
 *
 * <p>Interface de projection plutôt qu'entité : le tableau est une vue agrégée,
 * il ne correspond à aucune table et n'a pas vocation à être modifié.
 */
public interface LigneTableau {

    Long getEtudiantId();

    String getNom();

    long getPresences();

    long getExercicesDeposes();

    /** RG22 — {@code null} tant qu'aucune note n'a été reçue, jamais zéro. */
    Double getMoyenne();

    /** RG21 — ce que l'étudiant doit encore rendre <b>en tant que relecteur</b>. */
    long getRelecturesEnAttente();
}
