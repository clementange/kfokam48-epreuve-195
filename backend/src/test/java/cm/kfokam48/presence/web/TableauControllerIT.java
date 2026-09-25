package cm.kfokam48.presence.web;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EF8, Q16 — le tableau du formateur.
 *
 * <p>Le scénario est monté en SQL pour être entièrement déterministe : on veut
 * vérifier des agrégats exacts, pas les effets d'un tirage aléatoire.
 *
 * <pre>
 *   Awono   : 2 présences, 2 exercices, notes 12 et 17 → moyenne 14.5,
 *             1 relecture à rendre pour Bello
 *   Bello   : 1 présence,  1 exercice,  aucune note   → moyenne null,
 *             0 relecture en attente (il a rendu)
 *   Chendjou: 1 présence,  0 exercice,  aucune note   → moyenne null,
 *             1 relecture en attente
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TableauControllerIT {

    private static final Long PROMOTION = 1L;
    private static final Long AWONO = 1L;
    private static final Long BELLO = 2L;
    private static final Long CHENDJOU = 3L;

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @PersistenceContext private EntityManager em;

    private long compteur;

    private Long seance(String statut) {
        String code = "TAB" + String.format("%03d", ++compteur);
        Instant t = Instant.now();
        jdbc.update("""
                INSERT INTO session (titre, promotion_id, code, ouverture_at, expiration_at, statut, cloture_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "Séance " + code, PROMOTION, code,
                java.sql.Timestamp.from(t), java.sql.Timestamp.from(t.plus(15, ChronoUnit.MINUTES)),
                statut, "CLOTUREE".equals(statut) ? java.sql.Timestamp.from(t) : null);
        return jdbc.queryForObject("SELECT id FROM session WHERE code = ?", Long.class, code);
    }

    private void present(Long seance, Long etudiant) {
        jdbc.update("INSERT INTO presence (session_id, etudiant_id, source, marquee_at) VALUES (?, ?, 'ETUDIANT', ?)",
                seance, etudiant, java.sql.Timestamp.from(Instant.now()));
    }

    private Long exercice(Long seance, Long etudiant, String statut) {
        jdbc.update("INSERT INTO exercice (session_id, etudiant_id, lien, statut, depose_at) VALUES (?, ?, ?, ?, ?)",
                seance, etudiant, "https://github.com/e" + etudiant + "/s" + seance, statut,
                java.sql.Timestamp.from(Instant.now()));
        return jdbc.queryForObject("SELECT id FROM exercice WHERE session_id = ? AND etudiant_id = ?",
                Long.class, seance, etudiant);
    }

    private void relectureRendue(Long exercice, Long relecteur, int note) {
        jdbc.update("""
                INSERT INTO relecture (exercice_id, relecteur_id, statut, note, commentaire, assignee_at, rendue_at)
                VALUES (?, ?, 'RENDUE', ?, 'vu', ?, ?)
                """, exercice, relecteur, note,
                java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now()));
    }

    private void relectureEnAttente(Long exercice, Long relecteur) {
        jdbc.update("""
                INSERT INTO relecture (exercice_id, relecteur_id, statut, assignee_at)
                VALUES (?, ?, 'EN_ATTENTE', ?)
                """, exercice, relecteur, java.sql.Timestamp.from(Instant.now()));
    }

    @BeforeEach
    void monterLeScenario() {
        Long s1 = seance("CLOTUREE");
        Long s2 = seance("CLOTUREE");

        present(s1, AWONO);
        present(s1, BELLO);
        present(s1, CHENDJOU);
        present(s2, AWONO);

        Long exoAwono1 = exercice(s1, AWONO, "RELU");
        Long exoAwono2 = exercice(s2, AWONO, "RELU");
        Long exoBello = exercice(s1, BELLO, "EN_ATTENTE_RELECTURE");

        relectureRendue(exoAwono1, BELLO, 12);
        relectureRendue(exoAwono2, CHENDJOU, 17);
        relectureEnAttente(exoBello, AWONO);
        // Chendjou a aussi une relecture en attente sur un exercice de Bello ?
        // Non : un exercice n'a qu'un relecteur (RG15). On s'en tient là.

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("EF8 — les six champs du contrat, par étudiant")
    void tableauComplet() throws Exception {
        mvc.perform(get("/api/tableau").param("promotionId", PROMOTION.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].etudiantId").exists())
                .andExpect(jsonPath("$[0].nom").exists())
                .andExpect(jsonPath("$[0].presences").exists())
                .andExpect(jsonPath("$[0].exercicesDeposes").exists())
                .andExpect(jsonPath("$[0].relecturesEnAttente").exists());
    }

    @Test
    @DisplayName("RG22 — la moyenne des notes reçues : (12 + 17) / 2 = 14.5")
    void moyenneDesNotesRecues() throws Exception {
        mvc.perform(get("/api/tableau").param("promotionId", PROMOTION.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.etudiantId == 1)].presences")
                        .value(org.hamcrest.Matchers.hasItem(2)))
                .andExpect(jsonPath("$[?(@.etudiantId == 1)].exercicesDeposes")
                        .value(org.hamcrest.Matchers.hasItem(2)))
                .andExpect(jsonPath("$[?(@.etudiantId == 1)].moyenne")
                        .value(org.hamcrest.Matchers.hasItem(14.5)));
    }

    @Test
    @DisplayName("RG22 — moyenne null, et non zéro, pour un étudiant jamais relu")
    void moyenneNullSansNote() throws Exception {
        // Bello a déposé, mais sa relecture est encore en attente : il n'a reçu
        // aucune note. Afficher 0 lui mettrait un 0/20 qu'il n'a pas eu.
        mvc.perform(get("/api/tableau").param("promotionId", PROMOTION.toString()))
                .andExpect(jsonPath("$[?(@.etudiantId == 2)].moyenne")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath("$[?(@.etudiantId == 3)].moyenne")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())));
    }

    @Test
    @DisplayName("RG21, Q16 — relecturesEnAttente compte ce que l'étudiant doit rendre, pas ce qu'il attend")
    void relecturesEnAttenteCotesRelecteur() throws Exception {
        mvc.perform(get("/api/tableau").param("promotionId", PROMOTION.toString()))
                // Awono est relecteur de l'exercice de Bello, pas encore rendu
                .andExpect(jsonPath("$[?(@.etudiantId == 1)].relecturesEnAttente")
                        .value(org.hamcrest.Matchers.hasItem(1)))
                // Bello attend une relecture en tant qu'auteur, mais il a rendu
                // la sienne : son compteur doit rester à zéro.
                .andExpect(jsonPath("$[?(@.etudiantId == 2)].relecturesEnAttente")
                        .value(org.hamcrest.Matchers.hasItem(0)));
    }

    @Test
    @DisplayName("un étudiant sans aucune activité apparaît quand même, compteurs à zéro")
    void etudiantSansActivite() throws Exception {
        // Djeukam (4) n'a rien fait : il doit figurer au tableau. Un formateur
        // doit voir les absents, pas seulement les présents.
        mvc.perform(get("/api/tableau").param("promotionId", PROMOTION.toString()))
                .andExpect(jsonPath("$[?(@.etudiantId == 4)]")
                        .value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[?(@.etudiantId == 4)].presences")
                        .value(org.hamcrest.Matchers.hasItem(0)))
                .andExpect(jsonPath("$[?(@.etudiantId == 4)].moyenne")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())));
    }

    @Test
    @DisplayName("le tableau ne contient que les étudiants de la promotion demandée (RG24)")
    void cloisonnementParPromotion() throws Exception {
        mvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(jsonPath("$.length()").value(12));

        mvc.perform(get("/api/tableau").param("promotionId", "2"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("RG23 — 404 PROMOTION_INCONNUE, et non une liste vide")
    void promotionInconnue() throws Exception {
        mvc.perform(get("/api/tableau").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("400 REQUETE_INVALIDE quand le paramètre promotionId manque")
    void parametreManquant() throws Exception {
        mvc.perform(get("/api/tableau"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }
}
