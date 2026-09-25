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
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EF9, RG8 — le formateur ajoute une présence à la main (Q14).
 *
 * <p>Le test central est {@link #ajoutPossibleMemeQuandLeCodeAExpire()} : cette
 * opération n'a de sens que si elle fonctionne précisément là où le marquage par
 * code échoue.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PresenceManuelleIT {

    private static final Long PROMOTION = 1L;
    private static final Long AWONO = 1L;
    private static final Long MBARGA_AUTRE_PROMOTION = 13L;

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private long compteur;

    private Long seance(Instant ouverture, String statut) {
        String code = "MAN" + String.format("%03d", ++compteur);
        jdbc.update("""
                INSERT INTO session (titre, promotion_id, code, ouverture_at, expiration_at, statut, cloture_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "Séance " + code, PROMOTION, code,
                java.sql.Timestamp.from(ouverture),
                java.sql.Timestamp.from(ouverture.plus(15, ChronoUnit.MINUTES)),
                statut,
                "CLOTUREE".equals(statut) ? java.sql.Timestamp.from(ouverture.plus(2, ChronoUnit.HOURS)) : null);
        return jdbc.queryForObject("SELECT id FROM session WHERE code = ?", Long.class, code);
    }

    @BeforeEach
    void reinitialiser() {
        compteur = 0;
    }

    private ResultActions ajouter(Long sessionId, Long etudiantId) throws Exception {
        return mvc.perform(post("/api/presences/manuelles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(
                        Map.of("sessionId", sessionId, "etudiantId", etudiantId))));
    }

    @Test
    @DisplayName("EF9, RG8 — 201 avec source FORMATEUR, et la trace est en base")
    void ajoutNominal() throws Exception {
        Long seance = seance(Instant.now(), "OUVERTE");

        ajouter(seance, AWONO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(seance))
                .andExpect(jsonPath("$.etudiantId").value(AWONO))
                .andExpect(jsonPath("$.source").value("FORMATEUR"));

        // Q14 : « il faut que ça se voie ». La source n'est pas cosmétique,
        // elle doit être persistée.
        assertThat(jdbc.queryForObject(
                "SELECT source FROM presence WHERE session_id = ? AND etudiant_id = ?",
                String.class, seance, AWONO)).isEqualTo("FORMATEUR");
    }

    @Test
    @DisplayName("Q14 — l'ajout reste possible alors que le code a expiré : c'est sa raison d'être")
    void ajoutPossibleMemeQuandLeCodeAExpire() throws Exception {
        // Séance ouverte depuis 3 heures : le code ne marche plus depuis
        // longtemps (RG1). C'est exactement la situation que Q14 décrit — « un
        // souci de téléphone ». Refuser ici viderait l'opération de son sens.
        Long seance = seance(Instant.now().minus(3, ChronoUnit.HOURS), "OUVERTE");

        ajouter(seance, AWONO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("FORMATEUR"));
    }

    @Test
    @DisplayName("RG6 — 409 SESSION_CLOTUREE : une séance close ne reçoit plus rien")
    void ajoutRefuseSurSeanceCloturee() throws Exception {
        Long seance = seance(Instant.now(), "CLOTUREE");

        ajouter(seance, AWONO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    @DisplayName("RG3 — 409 DEJA_PRESENT si l'étudiant s'est déjà marqué lui-même")
    void ajoutRefuseSiDejaPresent() throws Exception {
        Long seance = seance(Instant.now(), "OUVERTE");
        jdbc.update("INSERT INTO presence (session_id, etudiant_id, source, marquee_at) VALUES (?, ?, 'ETUDIANT', ?)",
                seance, AWONO, java.sql.Timestamp.from(Instant.now()));

        ajouter(seance, AWONO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    @Test
    @DisplayName("RG24 — 400 ETUDIANT_HORS_PROMOTION, et cette fois le message est explicite")
    void ajoutRefusePourUneAutrePromotion() throws Exception {
        // Contrairement au marquage par code, qui répond CODE_INCONNU pour ne pas
        // confirmer l'existence d'un code, on nomme ici la raison du refus : le
        // formateur a le droit de savoir, il n'est pas en train de deviner.
        Long seance = seance(Instant.now(), "OUVERTE");

        ajouter(seance, MBARGA_AUTRE_PROMOTION)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ETUDIANT_HORS_PROMOTION"));
    }

    @Test
    @DisplayName("404 SESSION_INCONNUE si la séance n'existe pas")
    void seanceInconnue() throws Exception {
        ajouter(999_999L, AWONO)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    @DisplayName("400 ETUDIANT_INCONNU si l'étudiant n'existe pas")
    void etudiantInconnu() throws Exception {
        Long seance = seance(Instant.now(), "OUVERTE");

        ajouter(seance, 999_999L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    @DisplayName("400 REQUETE_INVALIDE quand la séance manque")
    void champManquant() throws Exception {
        mvc.perform(post("/api/presences/manuelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("etudiantId", AWONO))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }
}
