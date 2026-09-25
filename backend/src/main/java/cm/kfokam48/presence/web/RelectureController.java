package cm.kfokam48.presence.web;

import cm.kfokam48.presence.domaine.StatutRelecture;
import cm.kfokam48.presence.service.RelectureService;
import cm.kfokam48.presence.web.dto.MonExerciceDTO;
import cm.kfokam48.presence.web.dto.RelectureAssigneeDTO;
import cm.kfokam48.presence.web.dto.RendreRelectureRequete;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** {@code /api/relectures} et les vues personnelles de l'étudiant. */
@RestController
public class RelectureController {

    private final RelectureService service;

    public RelectureController(RelectureService service) {
        this.service = service;
    }

    /**
     * EF7 — {@code POST /api/relectures/{id}} → {@code 200}.
     *
     * <p>Erreurs : {@code 400} note hors 0–20, {@code 403} auto-relecture,
     * {@code 409} relecture déjà rendue.
     */
    @PostMapping("/api/relectures/{id}")
    public ResponseEntity<Void> rendre(@PathVariable Long id,
                                       @RequestBody @Valid RendreRelectureRequete requete) {
        service.rendre(id, requete.note(), requete.commentaire());
        return ResponseEntity.ok().build();
    }

    /** Écran relecteur — opération ajoutée au contrat. */
    @GetMapping("/api/etudiants/{id}/relectures")
    public List<RelectureAssigneeDTO> mesRelectures(@PathVariable Long id,
                                                    @RequestParam(required = false) StatutRelecture statut) {
        return service.mesRelectures(id, statut);
    }

    /** EF11, RG20 — opération ajoutée au contrat. */
    @GetMapping("/api/etudiants/{id}/exercices")
    public List<MonExerciceDTO> mesExercices(@PathVariable Long id) {
        return service.mesExercices(id);
    }
}
