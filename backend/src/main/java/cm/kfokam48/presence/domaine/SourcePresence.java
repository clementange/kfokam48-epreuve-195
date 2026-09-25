package cm.kfokam48.presence.domaine;

/**
 * RG8, Q14 — « il faut que ça se voie : marquez "ajouté par le formateur" ».
 *
 * <p>C'est la raison d'être du champ {@code source} du contrat : distinguer la
 * présence marquée par l'étudiant de celle ajoutée à la main par le formateur.
 */
public enum SourcePresence {
    ETUDIANT,
    FORMATEUR
}
