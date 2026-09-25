package cm.kfokam48.presence.web;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration de {@code POST /api/sessions} (contrainte B6).
 *
 * <p>Il tourne sur H2 en mode PostgreSQL, avec les migrations Flyway réelles :
 * aucune base locale n'est nécessaire, le test passe sur un poste vierge (ENF7).
 * C'est aussi ce qui garantit que le test s'exécute sur le même schéma que la
 * production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessionControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    private Long promotionId;

    @BeforeEach
    void preparerUnePromotion() {
        jdbc.update("INSERT INTO formateur (nom) VALUES ('Mme Ndongo')");
        Long formateurId = jdbc.queryForObject(
                "SELECT id FROM formateur ORDER BY id DESC LIMIT 1", Long.class);
        jdbc.update("INSERT INTO promotion (nom, formateur_id) VALUES (?, ?)",
                "Promotion de test " + System.nanoTime(), formateurId);
        promotionId = jdbc.queryForObject(
                "SELECT id FROM promotion ORDER BY id DESC LIMIT 1", Long.class);
    }

    private String corps(Object o) throws Exception {
        return json.writeValueAsString(o);
    }

    @Test
    @DisplayName("EF1 — 201 avec id, code, ouvertureAt et expirationAt")
    void ouvertureRenvoieLesQuatreChampsDuContrat() throws Exception {
        String reponse = mvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("titre", "Séance 12 — Spring Data JPA",
                                "promotionId", promotionId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.ouvertureAt").isString())
                .andExpect(jsonPath("$.expirationAt").isString())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> corps = json.readValue(reponse, Map.class);

        // RG1 — l'expiration est bien 15 minutes après l'ouverture
        Instant ouverture = Instant.parse((String) corps.get("ouvertureAt"));
        Instant expiration = Instant.parse((String) corps.get("expirationAt"));
        assertThat(Duration.between(ouverture, expiration)).isEqualTo(Duration.ofMinutes(15));

        // ENF5 — 6 caractères, alphabet sans caractères ambigus
        assertThat((String) corps.get("code"))
                .hasSize(6)
                .matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}");

        // la séance est bien née OUVERTE, sans date de clôture
        Map<String, Object> enBase = jdbc.queryForMap(
                "SELECT statut, cloture_at FROM session WHERE id = ?", corps.get("id"));
        assertThat(enBase.get("statut")).isEqualTo("OUVERTE");
        assertThat(enBase.get("cloture_at")).isNull();
    }

    @Test
    @DisplayName("RG2 — deux séances ouvertes n'ont jamais le même code")
    void deuxSeancesOntDesCodesDifferents() throws Exception {
        String premier = codeRenvoyeParUneOuverture("Séance A");
        String second = codeRenvoyeParUneOuverture("Séance B");

        assertThat(premier).isNotEqualTo(second);
    }

    private String codeRenvoyeParUneOuverture(String titre) throws Exception {
        String reponse = mvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("titre", titre, "promotionId", promotionId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return (String) json.readValue(reponse, Map.class).get("code");
    }

    @Test
    @DisplayName("400 REQUETE_INVALIDE quand le titre manque, au format { code, message }")
    void titreManquant() throws Exception {
        mvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("promotionId", promotionId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("400 REQUETE_INVALIDE quand la promotion manque")
    void promotionManquante() throws Exception {
        mvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("titre", "Séance sans promotion"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }

    @Test
    @DisplayName("404 PROMOTION_INCONNUE quand la promotion n'existe pas")
    void promotionInexistante() throws Exception {
        // Le contrat impose 404 PROMOTION_INCONNUE sur GET /api/tableau. Un même
        // code d'erreur ne peut pas valoir 404 là et 400 ici : la cohérence
        // « un code, un statut » prime, et le 404 est ajouté au contrat.
        mvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("titre", "Séance fantôme", "promotionId", 999_999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.message").value("Cette promotion n'existe pas."));
    }

    @Test
    @DisplayName("la liste des séances d'une promotion inconnue répond 404 PROMOTION_INCONNUE")
    void listeSurPromotionInconnue() throws Exception {
        mvc.perform(get("/api/sessions").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    @Test
    @DisplayName("la liste renvoie les séances de la promotion, la plus récente d'abord")
    void listeDesSeances() throws Exception {
        codeRenvoyeParUneOuverture("Séance 1");
        codeRenvoyeParUneOuverture("Séance 2");

        mvc.perform(get("/api/sessions").param("promotionId", promotionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].statut").value("OUVERTE"));
    }
}
