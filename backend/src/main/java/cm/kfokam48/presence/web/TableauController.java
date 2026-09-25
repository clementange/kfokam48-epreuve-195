package cm.kfokam48.presence.web;

import cm.kfokam48.presence.service.TableauService;
import cm.kfokam48.presence.web.dto.LigneTableauDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** {@code /api/tableau} — dernière opération imposée par le contrat. */
@RestController
@RequestMapping("/api/tableau")
public class TableauController {

    private final TableauService service;

    public TableauController(TableauService service) {
        this.service = service;
    }

    /** EF8 — {@code GET /api/tableau?promotionId=} → {@code 200 [ … ]}, {@code 404} promotion inconnue. */
    @GetMapping
    public List<LigneTableauDTO> tableau(@RequestParam Long promotionId) {
        return service.tableau(promotionId);
    }
}
