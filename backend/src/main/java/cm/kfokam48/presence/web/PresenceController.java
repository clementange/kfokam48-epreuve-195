package cm.kfokam48.presence.web;

import cm.kfokam48.presence.service.PresenceService;
import cm.kfokam48.presence.web.dto.MarquerPresenceRequete;
import cm.kfokam48.presence.web.dto.PresenceManuelleRequete;
import cm.kfokam48.presence.web.dto.PresenceDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** {@code /api/presences} — opération imposée par le contrat. */
@RestController
@RequestMapping("/api/presences")
public class PresenceController {

    private final PresenceService service;

    public PresenceController(PresenceService service) {
        this.service = service;
    }

    /**
     * EF2 — {@code POST /api/presences} → {@code 201 { id, sessionId, etudiantId, source }}.
     *
     * <p>Erreurs : {@code 400} code inconnu, {@code 409} déjà présent ou séance
     * clôturée, {@code 410} code expiré.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PresenceDTO marquer(@RequestBody @Valid MarquerPresenceRequete requete) {
        return PresenceDTO.de(service.marquer(requete.code(), requete.etudiantId()));
    }

    /**
     * EF9, RG8 — {@code POST /api/presences/manuelles} → {@code 201} avec
     * {@code source: "FORMATEUR"}.
     *
     * <p>Opération ajoutée au contrat, née de Q14.
     */
    @PostMapping("/manuelles")
    @ResponseStatus(HttpStatus.CREATED)
    public PresenceDTO ajouterParFormateur(@RequestBody @Valid PresenceManuelleRequete requete) {
        return PresenceDTO.de(
                service.ajouterParFormateur(requete.sessionId(), requete.etudiantId()));
    }
}
