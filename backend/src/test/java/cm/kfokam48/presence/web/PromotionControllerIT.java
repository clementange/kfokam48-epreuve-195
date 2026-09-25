package cm.kfokam48.presence.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EF3, RG25 — la liste dans laquelle l'étudiant se désigne, faute
 * d'authentification (Q1).
 *
 * <p>Le test s'appuie sur les données de démonstration de la migration V2 :
 * c'est aussi une façon de vérifier que le seed arrive bien avec le schéma,
 * ce dont dépend ENF6.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PromotionControllerIT {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("les promotions de démonstration sont livrées par la migration, avec leur formateur")
    void listeDesPromotions() throws Exception {
        mvc.perform(get("/api/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == 1)].nom")
                        .value(org.hamcrest.Matchers.hasItem("KFOKAM48 — Promotion Yaoundé 2026")))
                .andExpect(jsonPath("$[?(@.id == 1)].formateurNom")
                        .value(org.hamcrest.Matchers.hasItem("Mme Ndongo Clarisse")));
    }

    @Test
    @DisplayName("EF3 — la liste des étudiants d'une promotion, triée par nom")
    void listeDesEtudiants() throws Exception {
        mvc.perform(get("/api/promotions/1/etudiants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].nom").value("Awono Marie"))
                .andExpect(jsonPath("$[0].promotionId").value(1))
                .andExpect(jsonPath("$[11].nom").value("Lontsi Rodrigue"));
    }

    @Test
    @DisplayName("RG24 — un étudiant n'apparaît que dans sa propre promotion")
    void etudiantsCloisonnesParPromotion() throws Exception {
        // Mbarga Sylvie est en promotion 2 : elle ne doit pas apparaître en 1
        mvc.perform(get("/api/promotions/1/etudiants"))
                .andExpect(jsonPath("$[?(@.nom == 'Mbarga Sylvie')]")
                        .value(org.hamcrest.Matchers.empty()));

        mvc.perform(get("/api/promotions/2/etudiants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.nom == 'Mbarga Sylvie')]")
                        .value(org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("404 PROMOTION_INCONNUE au format { code, message }")
    void promotionInconnue() throws Exception {
        mvc.perform(get("/api/promotions/999999/etudiants"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.message").value("Cette promotion n'existe pas."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
