package cm.kfokam48.presence.service;

import java.net.URI;
import java.util.Set;

/**
 * RG10 — le lien d'un exercice doit être une adresse http ou https absolue.
 *
 * <p>La validation est faite ici plutôt que par une annotation {@code @URL} :
 * les validateurs génériques acceptent {@code ftp://}, {@code javascript:} ou
 * une adresse relative, qui n'ont aucun sens pour un travail d'étudiant et
 * seraient, pour le premier, une porte ouverte côté navigateur.
 */
public final class ValidateurLien {

    private static final Set<String> SCHEMAS_AUTORISES = Set.of("http", "https");
    private static final int LONGUEUR_MAXIMALE = 500;

    private ValidateurLien() {
    }

    public static boolean estValide(String lien) {
        if (lien == null || lien.isBlank() || lien.length() > LONGUEUR_MAXIMALE) {
            return false;
        }
        try {
            URI uri = URI.create(lien.trim());
            return uri.isAbsolute()
                    && uri.getScheme() != null
                    && SCHEMAS_AUTORISES.contains(uri.getScheme().toLowerCase())
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
