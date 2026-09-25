package cm.kfokam48.presence.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
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
 * EF2 — le marquage de présence, cas nominal et les cinq cas d'erreur.
 *
 * <p>Ce test est la contrepartie exécutable du diagramme D3 : chaque branche du
 * diagramme a son test, avec le code HTTP que le contrat impose. Si D3 et le
 * code divergent, c'est ici que ça se voit.
 *
 * <p>Les séances sont insérées directement en base plutôt qu'ouvertes par l'API,
 * afin de pouvoir fabriquer des situations que l'API ne permet pas d'atteindre :
 * une séance déjà expirée, ou déjà clôturée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PresenceControllerIT {

    private static final Long PROMOTION_YAOUNDE = 1L;
    private static final Long AWONO_MARIE = 1L;      // promotion 1
    private static final Long BELLO_IDRISS = 2L;     // promotion 1
    private static final Long MBARGA_SYLVIE = 13L;   // promotion 2

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    private long compteurDeCodes;

    @BeforeEach
    void reinitialiser() {
        compteurDeCodes = 0;
    }

    /** Insère une séance dans l'état voulu, y compris ceux que l'API interdit d'atteindre. */
    private String seance(Long promotionId, Instant ouverture, String statut) {
        String code = "TST" + String.format("%03d", ++compteurDeCodes);
        Instant expiration = ouverture.plus(15, ChronoUnit.MINUTES);
        jdbc.update("""
                INSERT INTO session (titre, promotion_id, code, ouverture_at, expiration_at, statut, cloture_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                "Séance de test " + code, promotionId, code,
                java.sql.Timestamp.from(ouverture),
                java.sql.Timestamp.from(expiration),
                statut,
                "CLOTUREE".equals(statut) ? java.sql.Timestamp.from(ouverture.plus(2, ChronoUnit.HOURS)) : null);
        return code;
    }

    private String seanceOuverteMaintenant(Long promotionId) {
        return seance(promotionId, Instant.now(), "OUVERTE");
    }

    private org.springframework.test.web.servlet.ResultActions marquer(String code, Long etudiantId)
            throws Exception {
        return mvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("code", code, "etudiantId", etudiantId))));
    }

    @Nested
    @DisplayName("cas nominal")
    class CasNominal {

        @Test
        @DisplayName("201 avec les quatre champs du contrat et source ETUDIANT")
        void presenceEnregistree() throws Exception {
            String code = seanceOuverteMaintenant(PROMOTION_YAOUNDE);

            marquer(code, AWONO_MARIE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.sessionId").isNumber())
                    .andExpect(jsonPath("$.etudiantId").value(AWONO_MARIE))
                    .andExpect(jsonPath("$.source").value("ETUDIANT"));

            Integer enBase = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM presence WHERE etudiant_id = ? AND source = 'ETUDIANT'",
                    Integer.class, AWONO_MARIE);
            assertThat(enBase).isEqualTo(1);
        }

        @Test
        @DisplayName("le code est accepté en minuscules et avec des espaces : il est recopié à la main")
        void codeNormalise() throws Exception {
            String code = seanceOuverteMaintenant(PROMOTION_YAOUNDE);

            marquer("  " + code.toLowerCase() + "  ", BELLO_IDRISS)
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("deux étudiants différents peuvent marquer la même séance")
        void deuxEtudiantsMemeSeance() throws Exception {
            String code = seanceOuverteMaintenant(PROMOTION_YAOUNDE);

            marquer(code, AWONO_MARIE).andExpect(status().isCreated());
            marquer(code, BELLO_IDRISS).andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("cas d'erreur — chaque branche du diagramme D3")
    class CasDErreur {

        @Test
        @DisplayName("400 CODE_INCONNU quand le code n'existe pas")
        void codeInexistant() throws Exception {
            marquer("ZZZZZZ", AWONO_MARIE)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }

        @Test
        @DisplayName("RG24 — 400 CODE_INCONNU quand le code appartient à une autre promotion")
        void codeDUneAutrePromotion() throws Exception {
            String codeYaounde = seanceOuverteMaintenant(PROMOTION_YAOUNDE);

            // Mbarga Sylvie est en promotion 2. Le code existe, il est valide, il
            // n'est pas expiré — mais il ne la concerne pas. On ne le lui confirme
            // pas : ce serait aider à deviner les codes (Q4).
            marquer(codeYaounde, MBARGA_SYLVIE)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CODE_INCONNU"));
        }

        @Test
        @DisplayName("400 ETUDIANT_INCONNU quand l'étudiant n'existe pas")
        void etudiantInexistant() throws Exception {
            String code = seanceOuverteMaintenant(PROMOTION_YAOUNDE);

            marquer(code, 999_999L)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
        }

        @Test
        @DisplayName("RG1, RG5 — 410 CODE_EXPIRE passé les 15 minutes")
        void codeExpire() throws Exception {
            String code = seance(PROMOTION_YAOUNDE,
                    Instant.now().minus(20, ChronoUnit.MINUTES), "OUVERTE");

            marquer(code, AWONO_MARIE)
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.code").value("CODE_EXPIRE"))
                    .andExpect(jsonPath("$.message").value("Le code de présence a expiré."));
        }

        @Test
        @DisplayName("RG6 — 409 SESSION_CLOTUREE sur une séance clôturée")
        void seanceCloturee() throws Exception {
            String code = seance(PROMOTION_YAOUNDE, Instant.now(), "CLOTUREE");

            marquer(code, AWONO_MARIE)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
        }

        @Test
        @DisplayName("la clôture l'emporte sur l'expiration : une séance close et expirée répond SESSION_CLOTUREE")
        void clotureAvantExpiration() throws Exception {
            // C'est le point d'attention documenté dans D3. Une séance clôturée le
            // reste définitivement ; un code expiré sur une séance ouverte laisse
            // encore au formateur la possibilité d'ajouter la présence à la main.
            String code = seance(PROMOTION_YAOUNDE,
                    Instant.now().minus(3, ChronoUnit.HOURS), "CLOTUREE");

            marquer(code, AWONO_MARIE)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
        }

        @Test
        @DisplayName("RG3 — 409 DEJA_PRESENT au second marquage")
        void dejaPresent() throws Exception {
            String code = seanceOuverteMaintenant(PROMOTION_YAOUNDE);
            marquer(code, AWONO_MARIE).andExpect(status().isCreated());

            marquer(code, AWONO_MARIE)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
        }

        @Test
        @DisplayName("400 REQUETE_INVALIDE quand le code est vide")
        void codeVide() throws Exception {
            marquer("", AWONO_MARIE)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
        }
    }
}
