package cm.kfokam48.presence.service;

/**
 * Produit le code de présence d'une séance.
 *
 * <p>Interface plutôt que méthode statique : le tirage est aléatoire, donc
 * intestable en l'état. L'isoler derrière un contrat permet de le remplacer par
 * une valeur fixe dans les tests, sans rendre le code de production conditionnel.
 */
public interface GenerateurCode {

    /** ENF5 — 6 caractères, sans I, O, 0 ni 1 qui se confondent à la lecture. */
    String genererCode();
}
