package cm.kfokam48.presence.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** EF4 et EF10 — dépôt du lien d'un exercice, et son remplacement (Q12, Q13). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExerciceControllerIT {

    private static final Long PROMOTION = 1L;
    private static final Long AWONO = 1L;
    private static final Long BELLO = 2L;
    private static final String LIEN = "https://github.com/awono/tp12";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    /**
     * Le test partage sa transaction avec le contrôleur (@Transactional sur la
     * classe). Une écriture JDBC directe reste donc invisible des entités déjà
     * chargées dans le contexte de persistance, et une écriture JPA n'est pas
     * encore en base pour une lecture JDBC. On synchronise explicitement.
     *
     * <p>Ce n'est pas un défaut de l'application : en production, chaque requête
     * HTTP ouvre sa propre transaction et le problème ne se pose pas.
     */
    @PersistenceContext private EntityManager em;

    private void synchroniser() {
        em.flush();
        em.clear();
    }

    private long compteur;

    @BeforeEach
    void reinitialiser() {
        compteur = 0;
    }

    private Long seance(Instant ouverture, String statut) {
        String code = "EXO" + String.format("%03d", ++compteur);
        jdbc.update("""
                INSERT INTO session (titre, promotion_id, code, ouverture_at, expiration_at, statut, cloture_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                "Séance " + code, PROMOTION, code,
                java.sql.Timestamp.from(ouverture),
                java.sql.Timestamp.from(ouverture.plus(15, ChronoUnit.MINUTES)),
                statut,
                "CLOTUREE".equals(statut) ? java.sql.Timestamp.from(ouverture.plus(2, ChronoUnit.HOURS)) : null);
        return jdbc.queryForObject("SELECT id FROM session WHERE code = ?", Long.class, code);
    }

    private void rendrePresent(Long sessionId, Long etudiantId) {
        jdbc.update("INSERT INTO presence (session_id, etudiant_id, source, marquee_at) VALUES (?, ?, 'ETUDIANT', ?)",
                sessionId, etudiantId, java.sql.Timestamp.from(Instant.now()));
    }

    private ResultActions deposer(Long sessionId, Long etudiantId, String lien) throws Exception {
        return mvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(
                        Map.of("sessionId", sessionId, "etudiantId", etudiantId, "lien", lien))));
    }

    @Test
    @DisplayName("EF4 — 201 { id, statut: DEPOSE } pour un étudiant présent")
    void depotNominal() throws Exception {
        Long seance = seance(Instant.now(), "OUVERTE");
        rendrePresent(seance, AWONO);

        deposer(seance, AWONO, LIEN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("DEPOSE"));
    }

    @Test
    @DisplayName("RG12, Q12 — le dépôt reste possible après l'expiration du code")
    void depotApresExpirationDuCode() throws Exception {
        // C'est le cœur de Q12 : « certains n'ont pas de connexion le soir même ».
        // L'expiration du code ne ferme pas le dépôt ; seule la clôture le fait.
        Long seance = seance(Instant.now().minus(4, ChronoUnit.HOURS), "OUVERTE");
        rendrePresent(seance, AWONO);

        deposer(seance, AWONO, LIEN).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RG12 — 409 SESSION_CLOTUREE une fois la séance clôturée")
    void depotSurSeanceCloturee() throws Exception {
        Long seance = seance(Instant.now(), "CLOTUREE");
        rendrePresent(seance, AWONO);

        deposer(seance, AWONO, LIEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    @DisplayName("RG11 — 400 ETUDIANT_ABSENT si l'étudiant n'a pas marqué sa présence")
    void depotSansPresence() throws Exception {
        Long seance = seance(Instant.now(), "OUVERTE");

        // Un absent n'est pas relisible : le relecteur est tiré parmi les présents (Q7).
        deposer(seance, AWONO, LIEN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ETUDIANT_ABSENT"));
    }

    @Test
    @DisplayName("RG10 — 400 LIEN_INVALIDE pour une adresse qui n'est pas http(s)")
    void lienInvalide() throws Exception {
        Long seance = seance(Instant.now(), "OUVERTE");
        rendrePresent(seance, AWONO);

        deposer(seance, AWONO, "javascript:alert(1)")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }

    @Test
    @DisplayName("RG9 — 409 EXERCICE_DEJA_DEPOSE au second dépôt")
    void secondDepot() throws Exception {
        Long seance = seance(Instant.now(), "OUVERTE");
        rendrePresent(seance, AWONO);
        deposer(seance, AWONO, LIEN).andExpect(status().isCreated());

        deposer(seance, AWONO, "https://github.com/awono/tp12-bis")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"));
    }

    @Test
    @DisplayName("404 SESSION_INCONNUE si la séance n'existe pas")
    void seanceInconnue() throws Exception {
        deposer(999_999L, AWONO, LIEN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    @DisplayName("deux étudiants présents peuvent déposer sur la même séance")
    void deuxDepotsDifferents() throws Exception {
        Long seance = seance(Instant.now(), "OUVERTE");
        rendrePresent(seance, AWONO);
        rendrePresent(seance, BELLO);

        deposer(seance, AWONO, LIEN).andExpect(status().isCreated());
        deposer(seance, BELLO, "https://github.com/bello/tp12").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("EF10, RG13 — le lien est remplaçable tant que la séance est ouverte")
    void remplacementDuLien() throws Exception {
        Long seance = seance(Instant.now(), "OUVERTE");
        rendrePresent(seance, AWONO);
        String reponse = deposer(seance, AWONO, LIEN)
                .andReturn().getResponse().getContentAsString();
        Object exerciceId = json.readValue(reponse, Map.class).get("id");

        mvc.perform(put("/api/exercices/" + exerciceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                Map.of("lien", "https://github.com/awono/tp12-corrige"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lien").value("https://github.com/awono/tp12-corrige"))
                .andExpect(jsonPath("$.statut").value("DEPOSE"))
                .andExpect(jsonPath("$.majAt").isNotEmpty());

        synchroniser();

        // RG13 — la date de dépôt initiale n'est pas écrasée par le remplacement
        Map<String, Object> enBase = jdbc.queryForMap(
                "SELECT depose_at, maj_at FROM exercice WHERE id = ?", exerciceId);
        assertThat(enBase.get("depose_at")).isNotNull();
        assertThat(enBase.get("maj_at")).isNotNull();
        assertThat(enBase.get("depose_at")).isNotEqualTo(enBase.get("maj_at"));
    }

    @Test
    @DisplayName("RG13 — 409 SESSION_CLOTUREE : après la clôture, un relecteur est assigné")
    void remplacementApresCloture() throws Exception {
        Long seance = seance(Instant.now(), "OUVERTE");
        rendrePresent(seance, AWONO);
        String reponse = deposer(seance, AWONO, LIEN)
                .andReturn().getResponse().getContentAsString();
        Object exerciceId = json.readValue(reponse, Map.class).get("id");

        synchroniser();
        jdbc.update("UPDATE session SET statut = 'CLOTUREE', cloture_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now()), seance);
        synchroniser();

        mvc.perform(put("/api/exercices/" + exerciceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("lien", "https://github.com/awono/trop-tard"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    @DisplayName("404 EXERCICE_INCONNU au remplacement d'un exercice inexistant")
    void remplacementExerciceInconnu() throws Exception {
        mvc.perform(put("/api/exercices/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("lien", LIEN))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INCONNU"));
    }
}
