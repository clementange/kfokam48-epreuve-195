package cm.kfokam48.presence.commun.erreur;

/**
 * Exception portant un {@link CodeErreur} du contrat d'API.
 *
 * <p>Les services métier lèvent cette exception ; ils n'ont jamais à connaître
 * le statut HTTP, qui est attaché au code. C'est le {@link GestionnaireErreurs}
 * qui la traduit en réponse.
 */
public class ErreurMetier extends RuntimeException {

    private final CodeErreur code;

    public ErreurMetier(CodeErreur code) {
        super(code.message());
        this.code = code;
    }

    /**
     * Variante avec un message circonstancié — utile lorsqu'un détail aide
     * l'utilisateur, par exemple le temps restant avant de pouvoir réessayer.
     */
    public ErreurMetier(CodeErreur code, String message) {
        super(message);
        this.code = code;
    }

    public CodeErreur code() {
        return code;
    }
}
