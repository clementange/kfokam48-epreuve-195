package cm.kfokam48.presence.web;

import cm.kfokam48.presence.service.TirageRelecteur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EF5, EF6 — la clôture et l'assignation qu'elle déclenche.
 *
 * <p>Le tirage aléatoire est remplacé par un tirage déterministe : on teste la
 * <em>règle</em> — le relecteur est présent et n'est jamais l'auteur — pas la
 * chance. Un test qui dépend du hasard ne prouve rien le jour où il passe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClotureSessionIT {

    private static final Long PROMOTION = 1L;
    private static final Long AWONO = 1L;
    private static final Long BELLO = 2L;
    private static final Long CHENDJOU = 3L;

    /** Tirage déterministe : toujours le premier candidat, dans l'ordre reçu. */
    @TestConfiguration
    static class TirageDeterministe {
        @Bean
        @Primary
        TirageRelecteur tirageDeterministe() {
            return candidats -> (candidats == null || candidats.isEmpty()) ? null : candidats.get(0);
        }
    }

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @PersistenceContext private EntityManager em;

    private long compteur;

    @BeforeEach
    void reinitialiser() {
        compteur = 0;
    }

    private void synchroniser() {
        em.flush();
        em.clear();
    }

    private Long seanceOuverte() {
        String code = "CLO" + String.format("%03d", ++compteur);
        Instant ouverture = Instant.now();
        jdbc.update("""
                INSERT INTO session (titre, promotion_id, code, ouverture_at, expiration_at, statut)
                VALUES (?, ?, ?, ?, ?, 'OUVERTE')
                """,
                "Séance " + code, PROMOTION, code,
                java.sql.Timestamp.from(ouverture),
                java.sql.Timestamp.from(ouverture.plus(15, ChronoUnit.MINUTES)));
        return jdbc.queryForObject("SELECT id FROM session WHERE code = ?", Long.class, code);
    }

    private void present(Long seance, Long etudiant) {
        jdbc.update("INSERT INTO presence (session_id, etudiant_id, source, marquee_at) VALUES (?, ?, 'ETUDIANT', ?)",
                seance, etudiant, java.sql.Timestamp.from(Instant.now()));
    }

    private Long exercice(Long seance, Long etudiant) {
        jdbc.update("""
                INSERT INTO exercice (session_id, etudiant_id, lien, statut, depose_at)
                VALUES (?, ?, ?, 'DEPOSE', ?)
                """,
                seance, etudiant, "https://github.com/e" + etudiant + "/tp",
                java.sql.Timestamp.from(Instant.now()));
        return jdbc.queryForObject(
                "SELECT id FROM exercice WHERE session_id = ? AND etudiant_id = ?",
                Long.class, seance, etudiant);
    }

    @Test
    @DisplayName("EF5 — 200 avec statut CLOTUREE, date et compteurs")
    void clotureNominale() throws Exception {
        Long seance = seanceOuverte();
        present(seance, AWONO);
        present(seance, BELLO);
        exercice(seance, AWONO);
        exercice(seance, BELLO);
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seance))
                .andExpect(jsonPath("$.statut").value("CLOTUREE"))
                .andExpect(jsonPath("$.clotureAt").isNotEmpty())
                .andExpect(jsonPath("$.relecturesAssignees").value(2))
                .andExpect(jsonPath("$.exercicesNonAssignes").value(0));
    }

    @Test
    @DisplayName("RG17 — le relecteur n'est jamais l'auteur de l'exercice")
    void relecteurJamaisAuteur() throws Exception {
        Long seance = seanceOuverte();
        present(seance, AWONO);
        present(seance, BELLO);
        present(seance, CHENDJOU);
        exercice(seance, AWONO);
        exercice(seance, BELLO);
        exercice(seance, CHENDJOU);
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture")).andExpect(status().isOk());
        synchroniser();

        List<java.util.Map<String, Object>> lignes = jdbc.queryForList("""
                SELECT e.etudiant_id AS auteur, r.relecteur_id AS relecteur
                FROM relecture r JOIN exercice e ON e.id = r.exercice_id
                WHERE e.session_id = ?
                """, seance);

        assertThat(lignes).hasSize(3);
        assertThat(lignes).allSatisfy(l ->
                assertThat(l.get("relecteur")).isNotEqualTo(l.get("auteur")));
    }

    @Test
    @DisplayName("Q7 — le relecteur est toujours un étudiant présent à la séance")
    void relecteurToujoursPresent() throws Exception {
        Long seance = seanceOuverte();
        present(seance, AWONO);
        present(seance, BELLO);
        exercice(seance, AWONO);
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture")).andExpect(status().isOk());
        synchroniser();

        Long relecteur = jdbc.queryForObject("""
                SELECT r.relecteur_id FROM relecture r
                JOIN exercice e ON e.id = r.exercice_id WHERE e.session_id = ?
                """, Long.class, seance);

        assertThat(relecteur).isIn(AWONO, BELLO).isNotEqualTo(AWONO);
    }

    @Test
    @DisplayName("RG16 — un seul étudiant présent : l'exercice devient NON_ASSIGNE, sans relecture")
    void aucunRelecteurEligible() throws Exception {
        // Le cas que le client n'avait pas prévu, et qui se produit à chaque
        // séance à un seul étudiant. Sans RG16, l'application planterait ou
        // assignerait l'auteur à lui-même, ce que Q5 interdit formellement.
        Long seance = seanceOuverte();
        present(seance, AWONO);
        Long exo = exercice(seance, AWONO);
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relecturesAssignees").value(0))
                .andExpect(jsonPath("$.exercicesNonAssignes").value(1));
        synchroniser();

        assertThat(jdbc.queryForObject("SELECT statut FROM exercice WHERE id = ?", String.class, exo))
                .isEqualTo("NON_ASSIGNE");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM relecture WHERE exercice_id = ?", Integer.class, exo))
                .isZero();
    }

    @Test
    @DisplayName("l'exercice assigné passe à EN_ATTENTE_RELECTURE, la relecture à EN_ATTENTE")
    void statutsApresAssignation() throws Exception {
        Long seance = seanceOuverte();
        present(seance, AWONO);
        present(seance, BELLO);
        Long exo = exercice(seance, AWONO);
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture")).andExpect(status().isOk());
        synchroniser();

        assertThat(jdbc.queryForObject("SELECT statut FROM exercice WHERE id = ?", String.class, exo))
                .isEqualTo("EN_ATTENTE_RELECTURE");
        assertThat(jdbc.queryForObject(
                "SELECT statut FROM relecture WHERE exercice_id = ?", String.class, exo))
                .isEqualTo("EN_ATTENTE");
    }

    @Test
    @DisplayName("RG14 — 409 SESSION_DEJA_CLOTUREE : la clôture est irréversible")
    void clotureDeuxFois() throws Exception {
        Long seance = seanceOuverte();
        present(seance, AWONO);
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture")).andExpect(status().isOk());
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_DEJA_CLOTUREE"));
    }

    @Test
    @DisplayName("404 SESSION_INCONNUE pour une séance qui n'existe pas")
    void seanceInconnue() throws Exception {
        mvc.perform(post("/api/sessions/999999/cloture"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    @DisplayName("une séance sans aucun exercice se clôture sans rien assigner")
    void clotureSansExercice() throws Exception {
        Long seance = seanceOuverte();
        present(seance, AWONO);
        present(seance, BELLO);
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relecturesAssignees").value(0))
                .andExpect(jsonPath("$.exercicesNonAssignes").value(0));
    }
}
