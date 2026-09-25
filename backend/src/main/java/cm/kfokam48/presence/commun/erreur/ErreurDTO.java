package cm.kfokam48.presence.commun.erreur;

/**
 * Le corps d'erreur imposé par le contrat, pour toutes les erreurs sans exception.
 *
 * @param code    identifiant stable en majuscules, exemple {@code CODE_EXPIRE}
 * @param message phrase lisible par un humain, en français
 */
public record ErreurDTO(String code, String message) {

    public static ErreurDTO de(CodeErreur code) {
        return new ErreurDTO(code.name(), code.message());
    }

    public static ErreurDTO de(CodeErreur code, String message) {
        return new ErreurDTO(code.name(), message);
    }
}
