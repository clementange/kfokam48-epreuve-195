package cm.kfokam48.presence.web;

import cm.kfokam48.presence.service.SessionService;
import cm.kfokam48.presence.web.dto.ClotureDTO;
import cm.kfokam48.presence.web.dto.OuvrirSessionRequete;
import cm.kfokam48.presence.web.dto.SessionDTO;
import cm.kfokam48.presence.web.dto.SessionOuverteDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code /api/sessions} — opération imposée par le contrat.
 *
 * <p>Le contrôleur ne fait que trois choses : valider l'entrée, déléguer au
 * service, traduire en DTO. Aucune règle de gestion, aucune requête base (B3).
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    /** EF1 — {@code POST /api/sessions} → {@code 201 { id, code, ouvertureAt, expirationAt }}. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionOuverteDTO ouvrir(@RequestBody @Valid OuvrirSessionRequete requete) {
        return SessionOuverteDTO.de(service.ouvrir(requete.titre(), requete.promotionId()));
    }

    /**
     * EF5, EF6 — {@code POST /api/sessions/{id}/cloture}.
     *
     * <p>Opération <b>ajoutée</b> au contrat : elle comble le trou principal de
     * la demande du client, qui parle de « clôturer la session » (Q10, Q12) sans
     * jamais dire ce que c'est ni comment on le fait.
     *
     * <p>Erreurs : {@code 404} séance inconnue, {@code 409} déjà clôturée.
     */
    @PostMapping("/{id}/cloture")
    public ClotureDTO cloturer(@PathVariable Long id) {
        return ClotureDTO.de(service.cloturer(id));
    }

    /** Opération ajoutée au contrat : alimente l'écran formateur. */
    @GetMapping
    public ResponseEntity<List<SessionDTO>> lister(@RequestParam Long promotionId) {
        return ResponseEntity.ok(
                service.listerParPromotion(promotionId).stream().map(SessionDTO::de).toList());
    }
}
