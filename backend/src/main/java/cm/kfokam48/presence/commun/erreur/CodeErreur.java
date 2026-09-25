package cm.kfokam48.presence.commun.erreur;

import org.springframework.http.HttpStatus;

/**
 * Les codes d'erreur du contrat d'API, avec le statut HTTP et le message
 * qui leur sont associés.
 *
 * <p>Le contrat impose un corps {@code { code, message }} pour <em>toutes</em>
 * les erreurs sans exception. Centraliser les codes ici garantit qu'un même
 * cas métier ne peut pas répondre 409 à un endroit et 400 à un autre.
 *
 * <p>Le libellé est en français et compréhensible sans connaissance technique
 * (ENF8) : il est destiné à l'étudiant ou au formateur, pas au développeur.
 */
public enum CodeErreur {

    // --- Requête ------------------------------------------------------------
    REQUETE_INVALIDE(HttpStatus.BAD_REQUEST, "La requête est incomplète ou mal formée."),

    // --- Référentiel --------------------------------------------------------
    PROMOTION_INCONNUE(HttpStatus.NOT_FOUND, "Cette promotion n'existe pas."),
    ETUDIANT_INCONNU(HttpStatus.BAD_REQUEST, "Cet étudiant n'existe pas."),
    SESSION_INCONNUE(HttpStatus.NOT_FOUND, "Cette session n'existe pas."),
    EXERCICE_INCONNU(HttpStatus.NOT_FOUND, "Cet exercice n'existe pas."),
    RELECTURE_INCONNUE(HttpStatus.NOT_FOUND, "Cette relecture n'existe pas."),

    // --- Présence (RG1, RG3 à RG7, RG24) ------------------------------------
    CODE_INCONNU(HttpStatus.BAD_REQUEST, "Ce code de présence ne correspond à aucune séance en cours."),
    CODE_EXPIRE(HttpStatus.GONE, "Le code de présence a expiré."),
    DEJA_PRESENT(HttpStatus.CONFLICT, "Votre présence est déjà enregistrée pour cette séance."),
    TROP_D_ESSAIS(HttpStatus.TOO_MANY_REQUESTS, "Trop de codes erronés. Patientez deux minutes avant de réessayer."),
    ETUDIANT_HORS_PROMOTION(HttpStatus.BAD_REQUEST, "Cet étudiant n'appartient pas à la promotion de cette séance."),

    // --- Session (RG6, RG14) ------------------------------------------------
    SESSION_CLOTUREE(HttpStatus.CONFLICT, "Cette séance est clôturée."),
    SESSION_DEJA_CLOTUREE(HttpStatus.CONFLICT, "Cette séance a déjà été clôturée."),

    // --- Exercice (RG9 à RG11) ----------------------------------------------
    LIEN_INVALIDE(HttpStatus.BAD_REQUEST, "Le lien doit être une adresse http ou https valide."),
    EXERCICE_DEJA_DEPOSE(HttpStatus.CONFLICT, "Vous avez déjà déposé un exercice pour cette séance."),
    ETUDIANT_ABSENT(HttpStatus.BAD_REQUEST, "Vous devez être présent à la séance pour y déposer un exercice."),

    // --- Relecture (RG17 à RG19) --------------------------------------------
    NOTE_INVALIDE(HttpStatus.BAD_REQUEST, "La note doit être un nombre entier compris entre 0 et 20."),
    AUTO_RELECTURE(HttpStatus.FORBIDDEN, "Vous ne pouvez pas relire votre propre exercice."),
    RELECTURE_DEJA_RENDUE(HttpStatus.CONFLICT, "Cette relecture a déjà été rendue et ne peut plus être modifiée."),

    // --- Filet de sécurité --------------------------------------------------
    ERREUR_INTERNE(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue.");

    private final HttpStatus statut;
    private final String message;

    CodeErreur(HttpStatus statut, String message) {
        this.statut = statut;
        this.message = message;
    }

    public HttpStatus statut() {
        return statut;
    }

    public String message() {
        return message;
    }
}
