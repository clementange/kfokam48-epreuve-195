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

    /** Tirage déterministe : les premiers candidats dans l'ordre reçu, sans hasard. */
    @TestConfiguration
    static class TirageDeterministe {
        @Bean
        @Primary
        TirageRelecteur tirageDeterministe() {
            return (candidats, combien) -> (candidats == null || candidats.isEmpty())
                    ? java.util.List.of()
                    : java.util.List.copyOf(candidats.subList(0, Math.min(combien, candidats.size())));
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
    @DisplayName("EF5, issue #34 — trois présents : chaque exercice reçoit DEUX relecteurs")
    void clotureNominaleAvecDeuxRelecteurs() throws Exception {
        Long seance = seanceOuverte();
        present(seance, AWONO);
        present(seance, BELLO);
        present(seance, CHENDJOU);
        exercice(seance, AWONO);
        exercice(seance, BELLO);
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seance))
                .andExpect(jsonPath("$.statut").value("CLOTUREE"))
                .andExpect(jsonPath("$.clotureAt").isNotEmpty())
                // 2 exercices x 2 relecteurs
                .andExpect(jsonPath("$.relecturesAssignees").value(4))
                .andExpect(jsonPath("$.exercicesNonAssignes").value(0))
                .andExpect(jsonPath("$.exercicesUnSeulRelecteur").value(0));
    }

    @Test
    @DisplayName("issue #34 — deux présents seulement : un seul relecteur possible, et c'est signalé")
    void clotureAvecUnSeulRelecteurPossible() throws Exception {
        // Deux présents, l'auteur exclu : il ne reste qu'un candidat. L'exercice
        // aura une note, mais pas de moyenne — le formateur doit le savoir.
        Long seance = seanceOuverte();
        present(seance, AWONO);
        present(seance, BELLO);
        exercice(seance, AWONO);
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relecturesAssignees").value(1))
                .andExpect(jsonPath("$.exercicesUnSeulRelecteur").value(1))
                .andExpect(jsonPath("$.exercicesNonAssignes").value(0));
    }

    @Test
    @DisplayName("RG17 — aucun des deux relecteurs n'est l'auteur, et ils sont distincts")
    void relecteursJamaisAuteurEtDistincts() throws Exception {
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
                SELECT e.etudiant_id AS auteur, r.relecteur_id AS relecteur, r.exercice_id AS exercice
                FROM relecture r JOIN exercice e ON e.id = r.exercice_id
                WHERE e.session_id = ?
                """, seance);

        // 3 exercices x 2 relecteurs
        assertThat(lignes).hasSize(6);
        assertThat(lignes).allSatisfy(l ->
                assertThat(l.get("relecteur")).isNotEqualTo(l.get("auteur")));

        // issue #34 — « deux pairs DIFFÉRENTS » : un même relecteur ne peut pas
        // être assigné deux fois au même exercice.
        assertThat(lignes.stream().map(l -> l.get("exercice") + ":" + l.get("relecteur")).distinct())
                .as("aucun couple (exercice, relecteur) en double")
                .hasSize(6);
    }

    @Test
    @DisplayName("Q7 — les relecteurs sont toujours des étudiants présents à la séance")
    void relecteursToujoursPresents() throws Exception {
        Long seance = seanceOuverte();
        present(seance, AWONO);
        present(seance, BELLO);
        present(seance, CHENDJOU);
        exercice(seance, AWONO);
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture")).andExpect(status().isOk());
        synchroniser();

        List<Long> relecteurs = jdbc.queryForList("""
                SELECT r.relecteur_id FROM relecture r
                JOIN exercice e ON e.id = r.exercice_id WHERE e.session_id = ?
                """, Long.class, seance);

        assertThat(relecteurs).hasSize(2).containsExactlyInAnyOrder(BELLO, CHENDJOU)
                .doesNotContain(AWONO);
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
    @DisplayName("l'exercice assigné passe à EN_ATTENTE_RELECTURE, ses deux relectures à EN_ATTENTE")
    void statutsApresAssignation() throws Exception {
        Long seance = seanceOuverte();
        present(seance, AWONO);
        present(seance, BELLO);
        present(seance, CHENDJOU);
        Long exo = exercice(seance, AWONO);
        synchroniser();

        mvc.perform(post("/api/sessions/" + seance + "/cloture")).andExpect(status().isOk());
        synchroniser();

        assertThat(jdbc.queryForObject("SELECT statut FROM exercice WHERE id = ?", String.class, exo))
                .isEqualTo("EN_ATTENTE_RELECTURE");
        assertThat(jdbc.queryForList(
                "SELECT statut FROM relecture WHERE exercice_id = ?", String.class, exo))
                .hasSize(2).containsOnly("EN_ATTENTE");
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
