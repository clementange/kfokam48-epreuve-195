package cm.kfokam48.presence.service;

import cm.kfokam48.presence.repository.EtudiantRepository;
import cm.kfokam48.presence.web.dto.LigneTableauDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** EF8, Q16 — le récapitulatif du formateur, par promotion. */
@Service
@Transactional(readOnly = true)
public class TableauService {

    private final EtudiantRepository etudiants;
    private final ReferentielService referentiel;

    public TableauService(EtudiantRepository etudiants, ReferentielService referentiel) {
        this.etudiants = etudiants;
        this.referentiel = referentiel;
    }

    public List<LigneTableauDTO> tableau(Long promotionId) {
        // RG23 — une promotion inexistante répond 404, et non une liste vide :
        // « aucun étudiant » et « cette promotion n'existe pas » sont deux
        // informations différentes pour celui qui appelle.
        referentiel.exigerPromotionExistante(promotionId);

        return etudiants.tableauDeLaPromotion(promotionId).stream()
                .map(LigneTableauDTO::de)
                .toList();
    }
}
