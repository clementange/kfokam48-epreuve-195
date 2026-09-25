package cm.kfokam48.presence.commun.web;

import cm.kfokam48.presence.commun.erreur.CodeErreur;
import cm.kfokam48.presence.commun.erreur.ErreurDTO;
import cm.kfokam48.presence.commun.erreur.ErreurMetier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Gestion centralisée des erreurs (contrainte B4).
 *
 * <p>Toute erreur qui sort de l'application passe par ici et ressort au format
 * {@code { code, message }}. Aucune stack trace, aucun corps vide, jamais la
 * page d'erreur par défaut de Spring — le contrat est explicite : ces trois cas
 * valent zéro.
 *
 * <p>Le dernier gestionnaire attrape {@link Exception} : c'est lui qui garantit
 * qu'aucune exception imprévue ne peut échapper au format imposé.
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreurs.class);

    /** Les erreurs métier : le statut HTTP est porté par le code lui-même. */
    @ExceptionHandler(ErreurMetier.class)
    public ResponseEntity<ErreurDTO> erreurMetier(ErreurMetier e) {
        return ResponseEntity.status(e.code().statut())
                .body(ErreurDTO.de(e.code(), e.getMessage()));
    }

    /**
     * Validation d'un corps de requête annoté {@code @Valid}.
     *
     * <p>Les champs fautifs sont listés dans le message, parce qu'un simple
     * « requête invalide » n'aide personne à corriger son appel (ENF8).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurDTO> validation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " : " + err.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(" ; "));
        String message = details.isBlank()
                ? CodeErreur.REQUETE_INVALIDE.message()
                : CodeErreur.REQUETE_INVALIDE.message() + " " + details;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErreurDTO.de(CodeErreur.REQUETE_INVALIDE, message));
    }

    /** JSON illisible, paramètre obligatoire absent, type incompatible. */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErreurDTO> requeteIllisible(Exception e) {
        log.debug("Requête rejetée : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErreurDTO.de(CodeErreur.REQUETE_INVALIDE));
    }

    /**
     * Chemin inexistant. Sans ce gestionnaire, Spring renvoie sa propre page
     * d'erreur, qui ne respecte pas le format imposé.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErreurDTO> introuvable(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErreurDTO("RESSOURCE_INCONNUE", "Cette adresse n'existe pas."));
    }

    /**
     * Filet de sécurité. L'exception est journalisée en entier côté serveur,
     * mais le client ne reçoit qu'un code et une phrase : une stack trace
     * renvoyée au client est une faute.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurDTO> imprevue(Exception e) {
        log.error("Erreur non gérée", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErreurDTO.de(CodeErreur.ERREUR_INTERNE));
    }
}
