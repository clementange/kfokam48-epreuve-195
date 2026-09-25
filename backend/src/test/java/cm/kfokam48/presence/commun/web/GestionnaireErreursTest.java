package cm.kfokam48.presence.commun.web;

import cm.kfokam48.presence.commun.erreur.CodeErreur;
import cm.kfokam48.presence.commun.erreur.ErreurMetier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie la contrainte B4 et l'exigence ENF4 : toute erreur, sans exception,
 * sort au format { code, message }, avec le statut HTTP du contrat.
 *
 * <p>Le test s'appuie sur un contrôleur factice plutôt que sur un endpoint réel :
 * le format d'erreur est une propriété de l'application entière, pas d'une
 * opération particulière. Il doit rester vrai avant même qu'un seul endpoint
 * métier existe.
 */
class GestionnaireErreursTest {

    /** Contrôleur factice : il n'existe que pour provoquer chaque famille d'erreur. */
    @RestController
    static class ControleurFactice {

        record Corps(@NotBlank String titre, @NotNull Long promotionId) {}

        @PostMapping(path = "/factice/metier", produces = MediaType.APPLICATION_JSON_VALUE)
        void metier(@RequestParam CodeErreur code) {
            throw new ErreurMetier(code);
        }

        @PostMapping(path = "/factice/valide", consumes = MediaType.APPLICATION_JSON_VALUE)
        void valide(@RequestBody @Valid Corps corps) {
        }

        @PostMapping(path = "/factice/explose")
        void explose() {
            throw new IllegalStateException("panne simulée, ne doit jamais fuiter au client");
        }
    }

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ControleurFactice())
            .setControllerAdvice(new GestionnaireErreurs())
            .build();

    @ParameterizedTest(name = "{0} -> le statut et le format du contrat")
    @EnumSource(value = CodeErreur.class, names = {
            "CODE_INCONNU", "CODE_EXPIRE", "DEJA_PRESENT", "SESSION_CLOTUREE",
            "TROP_D_ESSAIS", "AUTO_RELECTURE", "RELECTURE_DEJA_RENDUE",
            "NOTE_INVALIDE", "LIEN_INVALIDE", "PROMOTION_INCONNUE"
    })
    @DisplayName("chaque erreur métier répond avec son statut et le corps { code, message }")
    void erreurMetierRespecteLeContrat(CodeErreur code) throws Exception {
        mvc.perform(post("/factice/metier").param("code", code.name()))
                .andExpect(status().is(code.statut().value()))
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.message").value(code.message()))
                // le contrat n'autorise que ces deux champs : ni trace, ni timestamp Spring
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    @DisplayName("un corps invalide répond 400 REQUETE_INVALIDE et nomme les champs fautifs")
    void corpsInvalide() throws Exception {
        mvc.perform(post("/factice/valide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("titre")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("promotionId")));
    }

    @Test
    @DisplayName("un JSON illisible répond 400 REQUETE_INVALIDE")
    void jsonIllisible() throws Exception {
        mvc.perform(post("/factice/valide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ceci n'est pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }

    @Test
    @DisplayName("une exception imprévue répond 500 ERREUR_INTERNE sans laisser fuiter le détail")
    void exceptionImprevue() throws Exception {
        mvc.perform(post("/factice/explose"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERREUR_INTERNE"))
                .andExpect(jsonPath("$.message").value(CodeErreur.ERREUR_INTERNE.message()))
                // le message technique reste côté serveur
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("panne simulée"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
