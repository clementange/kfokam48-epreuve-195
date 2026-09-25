package cm.kfokam48.presence.service;

import cm.kfokam48.presence.commun.erreur.CodeErreur;
import cm.kfokam48.presence.commun.erreur.ErreurMetier;
import cm.kfokam48.presence.domaine.Etudiant;
import cm.kfokam48.presence.domaine.Formateur;
import cm.kfokam48.presence.domaine.Promotion;
import cm.kfokam48.presence.repository.EtudiantRepository;
import cm.kfokam48.presence.repository.FormateurRepository;
import cm.kfokam48.presence.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lecture du référentiel : promotions et étudiants.
 *
 * <p>Ces données ne sont ni créées ni modifiées par l'application : leur gestion
 * est hors périmètre (cahier des charges §3). Elles arrivent par la migration de
 * démonstration.
 */
@Service
@Transactional(readOnly = true)
public class ReferentielService {

    private final PromotionRepository promotions;
    private final EtudiantRepository etudiants;
    private final FormateurRepository formateurs;

    public ReferentielService(PromotionRepository promotions,
                              EtudiantRepository etudiants,
                              FormateurRepository formateurs) {
        this.promotions = promotions;
        this.etudiants = etudiants;
        this.formateurs = formateurs;
    }

    public List<Promotion> listerPromotions() {
        return promotions.findAll();
    }

    /**
     * Les noms des formateurs, indexés par identifiant.
     *
     * <p>Une seule requête pour toutes les promotions, plutôt qu'une par ligne :
     * le référentiel est petit, mais la habitude du N+1 se prend vite.
     */
    public Map<Long, String> nomsDesFormateurs() {
        return formateurs.findAll().stream()
                .collect(Collectors.toMap(Formateur::getId, Formateur::getNom));
    }

    /** EF3, RG25 — la liste dans laquelle l'étudiant se désigne. */
    public List<Etudiant> listerEtudiants(Long promotionId) {
        exigerPromotionExistante(promotionId);
        return etudiants.findByPromotionIdOrderByNomAsc(promotionId);
    }

    public Etudiant exigerEtudiantExistant(Long etudiantId) {
        return etudiants.findById(etudiantId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.ETUDIANT_INCONNU));
    }

    public void exigerPromotionExistante(Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw new ErreurMetier(CodeErreur.PROMOTION_INCONNUE);
        }
    }
}
