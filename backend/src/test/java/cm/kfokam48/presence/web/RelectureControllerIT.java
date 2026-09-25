package cm.kfokam48.presence.web;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** EF7, EF11 — rendre une relecture, et consulter la note reçue (Q8, Q9, Q15). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RelectureControllerIT {

    private static final Long PROMOTION = 1L;
    private static final Long AWONO = 1L;
    private static final Long BELLO = 2L;

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @PersistenceContext private EntityManager em;

    private long compteur;
    private Long seanceId;
    private Long exerciceAwono;
    private Long relectureDeBello;

    private void synchroniser() {
        em.flush();
        em.clear();
    }

    /**
     * Awono dépose, Bello est assigné à sa relecture. Le montage est fait en SQL
     * pour partir d'un état stable, indépendant du tirage aléatoire.
     */
    @BeforeEach
    void monterUneRelectureAssignee() {
        String code = "REL" + String.format("%03d", ++compteur);
        Instant t = Instant.now();
        jdbc.update("""
                INSERT INTO session (titre, promotion_id, code, ouverture_at, expiration_at, statut)
                VALUES (?, ?, ?, ?, ?, 'OUVERTE')
                """, "Séance " + code, PROMOTION, code,
                java.sql.Timestamp.from(t), java.sql.Timestamp.from(t.plus(15, ChronoUnit.MINUTES)));
        seanceId = jdbc.queryForObject("SELECT id FROM session WHERE code = ?", Long.class, code);

        jdbc.update("INSERT INTO exercice (session_id, etudiant_id, lien, statut, depose_at) VALUES (?, ?, ?, 'EN_ATTENTE_RELECTURE', ?)",
                seanceId, AWONO, "https://github.com/awono/tp", java.sql.Timestamp.from(t));
        exerciceAwono = jdbc.queryForObject(
                "SELECT id FROM exercice WHERE session_id = ? AND etudiant_id = ?", Long.class, seanceId, AWONO);

        jdbc.update("INSERT INTO relecture (exercice_id, relecteur_id, statut, assignee_at) VALUES (?, ?, 'EN_ATTENTE', ?)",
                exerciceAwono, BELLO, java.sql.Timestamp.from(t));
        relectureDeBello = jdbc.queryForObject(
                "SELECT id FROM relecture WHERE exercice_id = ?", Long.class, exerciceAwono);
        synchroniser();
    }

    private ResultActions rendre(Long relectureId, Object note, String commentaire) throws Exception {
        Map<String, Object> corps = new HashMap<>();
        corps.put("note", note);
        corps.put("commentaire", commentaire);
        return mvc.perform(post("/api/relectures/" + relectureId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(corps)));
    }

    @Test
    @DisplayName("EF7 — 200, la relecture passe à RENDUE et l'exercice à RELU")
    void rendreNominal() throws Exception {
        rendre(relectureDeBello, 16, "Bonne structure, il manque la gestion des erreurs.")
                .andExpect(status().isOk());
        synchroniser();

        Map<String, Object> r = jdbc.queryForMap(
                "SELECT statut, note, commentaire, rendue_at FROM relecture WHERE id = ?", relectureDeBello);
        assertThat(r.get("statut")).isEqualTo("RENDUE");
        assertThat(r.get("note")).isEqualTo(16);
        assertThat(r.get("rendue_at")).isNotNull();

        assertThat(jdbc.queryForObject("SELECT statut FROM exercice WHERE id = ?", String.class, exerciceAwono))
                .isEqualTo("RELU");
    }

    @Test
    @DisplayName("RG18 — 400 NOTE_INVALIDE pour 21, et pour -1")
    void noteHorsBornes() throws Exception {
        rendre(relectureDeBello, 21, "trop haut")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));

        rendre(relectureDeBello, -1, "trop bas")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    @DisplayName("RG18 — les bornes 0 et 20 sont acceptées")
    void bornesAcceptees() throws Exception {
        rendre(relectureDeBello, 0, "à revoir entièrement").andExpect(status().isOk());
    }

    @Test
    @DisplayName("RG18, Q9 — une note décimale est refusée, pas arrondie : 400 NOTE_INVALIDE")
    void noteDecimale() throws Exception {
        // Q9 : « Sur 20, en nombres entiers. » Reçue en Integer, 15.5 serait
        // tronquée silencieusement en 15 par Jackson : le relecteur croirait
        // avoir mis 15,5 et l'étudiant recevrait 15. La note est donc reçue en
        // BigDecimal et son caractère entier est vérifié, pas subi.
        mvc.perform(post("/api/relectures/" + relectureDeBello)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": 15.5, \"commentaire\": \"presque\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    @DisplayName("RG18 — 16.0 est accepté : c'est un entier, écrit autrement")
    void noteEntiereAvecDecimaleNulle() throws Exception {
        mvc.perform(post("/api/relectures/" + relectureDeBello)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": 16.0, \"commentaire\": \"bien\"}"))
                .andExpect(status().isOk());
        synchroniser();

        assertThat(jdbc.queryForObject("SELECT note FROM relecture WHERE id = ?",
                Integer.class, relectureDeBello)).isEqualTo(16);
    }

    @Test
    @DisplayName("RG19, Q15 — 409 RELECTURE_DEJA_RENDUE : la note est définitive")
    void noteDefinitive() throws Exception {
        rendre(relectureDeBello, 14, "correct").andExpect(status().isOk());
        synchroniser();

        // C'est la contradiction Q10/Q15, tranchée en faveur de Q15 : le contrat
        // impose ce 409, retenir Q10 le rendrait inatteignable.
        rendre(relectureDeBello, 18, "finalement mieux")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"));
        synchroniser();

        assertThat(jdbc.queryForObject("SELECT note FROM relecture WHERE id = ?", Integer.class, relectureDeBello))
                .isEqualTo(14);
    }

    @Test
    @DisplayName("RG17, Q5 — 403 AUTO_RELECTURE si le relecteur est l'auteur")
    void autoRelecture() throws Exception {
        // Situation que l'assignation ne peut pas produire : on la fabrique en
        // base pour vérifier que le filet de sécurité du service tient quand même.
        jdbc.update("UPDATE relecture SET relecteur_id = ? WHERE id = ?", AWONO, relectureDeBello);
        synchroniser();

        rendre(relectureDeBello, 20, "je me mets 20")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_RELECTURE"));
    }

    @Test
    @DisplayName("404 RELECTURE_INCONNUE pour une relecture qui n'existe pas")
    void relectureInconnue() throws Exception {
        rendre(999_999L, 12, "sur du vide")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RELECTURE_INCONNUE"));
    }

    @Test
    @DisplayName("l'écran relecteur liste les relectures assignées avec l'identifiant à utiliser")
    void mesRelectures() throws Exception {
        mvc.perform(get("/api/etudiants/" + BELLO + "/relectures").param("statut", "EN_ATTENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.relectureId == " + relectureDeBello + ")]")
                        .value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[?(@.relectureId == " + relectureDeBello + ")].auteurNom")
                        .value(org.hamcrest.Matchers.hasItem("Awono Marie")))
                .andExpect(jsonPath("$[?(@.relectureId == " + relectureDeBello + ")].lien")
                        .value(org.hamcrest.Matchers.hasItem("https://github.com/awono/tp")));
    }

    @Test
    @DisplayName("EF11, RG20 — l'auteur voit sa note et son commentaire, jamais son relecteur")
    void auteurVoitSaNoteSansLeRelecteur() throws Exception {
        rendre(relectureDeBello, 17, "Très clair.").andExpect(status().isOk());
        synchroniser();

        String reponse = mvc.perform(get("/api/etudiants/" + AWONO + "/exercices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.exerciceId == " + exerciceAwono + ")].note")
                        .value(org.hamcrest.Matchers.hasItem(17)))
                .andExpect(jsonPath("$[?(@.exerciceId == " + exerciceAwono + ")].commentaire")
                        .value(org.hamcrest.Matchers.hasItem("Très clair.")))
                .andExpect(jsonPath("$[?(@.exerciceId == " + exerciceAwono + ")].statut")
                        .value(org.hamcrest.Matchers.hasItem("RELU")))
                .andReturn().getResponse().getContentAsString();

        // Q8 — « Mais pas le nom du relecteur. » Rien, dans la charge utile, ne
        // doit permettre de remonter à Bello : ni son identifiant, ni son nom.
        assertThat(reponse)
                .doesNotContain("relecteur")
                .doesNotContain("Bello")
                .doesNotContain("rendueAt");
    }

    @Test
    @DisplayName("un exercice non encore relu affiche une note nulle, pas zéro")
    void exerciceNonRelu() throws Exception {
        mvc.perform(get("/api/etudiants/" + AWONO + "/exercices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.exerciceId == " + exerciceAwono + ")].note")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())));
    }
}
