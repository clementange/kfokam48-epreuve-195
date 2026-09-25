package cm.kfokam48.presence.web;

import cm.kfokam48.presence.service.ExerciceService;
import cm.kfokam48.presence.web.dto.DeposerExerciceRequete;
import cm.kfokam48.presence.web.dto.ExerciceDTO;
import cm.kfokam48.presence.web.dto.ExerciceDeposeDTO;
import cm.kfokam48.presence.web.dto.RemplacerLienRequete;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** {@code /api/exercices} — dépôt imposé par le contrat, remplacement ajouté (Q13). */
@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService service;

    public ExerciceController(ExerciceService service) {
        this.service = service;
    }

    /** EF4 — {@code POST /api/exercices} → {@code 201 { id, statut }}. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciceDeposeDTO deposer(@RequestBody @Valid DeposerExerciceRequete requete) {
        return ExerciceDeposeDTO.de(
                service.deposer(requete.sessionId(), requete.etudiantId(), requete.lien()));
    }

    /** EF10, RG13 — {@code PUT /api/exercices/{id}} → {@code 200}. */
    @PutMapping("/{id}")
    public ExerciceDTO remplacerLien(@PathVariable Long id,
                                     @RequestBody @Valid RemplacerLienRequete requete) {
        return ExerciceDTO.de(service.remplacerLien(id, requete.lien()));
    }
}
