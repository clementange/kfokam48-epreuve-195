package cm.kfokam48.presence.web;

import cm.kfokam48.presence.service.ReferentielService;
import cm.kfokam48.presence.web.dto.EtudiantDTO;
import cm.kfokam48.presence.web.dto.PromotionDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * {@code /api/promotions} — opérations ajoutées au contrat.
 *
 * <p>Elles existent parce que Q1 supprime l'authentification : l'étudiant se
 * désigne dans une liste, encore faut-il que le client puisse l'obtenir.
 */
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final ReferentielService service;

    public PromotionController(ReferentielService service) {
        this.service = service;
    }

    @GetMapping
    public List<PromotionDTO> lister() {
        Map<Long, String> formateurs = service.nomsDesFormateurs();
        return service.listerPromotions().stream()
                .map(p -> PromotionDTO.de(p, formateurs.get(p.getFormateurId())))
                .toList();
    }

    /** EF3, RG25 — {@code GET /api/promotions/{id}/etudiants}. */
    @GetMapping("/{id}/etudiants")
    public List<EtudiantDTO> listerEtudiants(@PathVariable Long id) {
        return service.listerEtudiants(id).stream().map(EtudiantDTO::de).toList();
    }
}
