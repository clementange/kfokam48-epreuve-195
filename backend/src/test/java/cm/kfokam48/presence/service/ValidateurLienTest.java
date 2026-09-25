package cm.kfokam48.presence.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** RG10 — le lien doit être une adresse http ou https absolue. */
class ValidateurLienTest {

    @ParameterizedTest(name = "accepte {0}")
    @ValueSource(strings = {
            "https://github.com/exemple/tp12",
            "http://exemple.cm/tp",
            "https://gitlab.com/u/projet/-/tree/main",
            "https://drive.google.com/file/d/abc123/view?usp=sharing",
            "HTTPS://EXEMPLE.COM/TP"
    })
    void liensValides(String lien) {
        assertThat(ValidateurLien.estValide(lien)).isTrue();
    }

    @ParameterizedTest(name = "refuse {0}")
    @ValueSource(strings = {
            "github.com/exemple",            // pas de schéma : adresse relative
            "/tp12",                         // chemin relatif
            "ftp://exemple.cm/tp",           // schéma non autorisé
            "javascript:alert(1)",           // porte ouverte côté navigateur
            "file:///home/etudiant/tp.pdf",  // fichier local, inaccessible au relecteur
            "https://",                      // pas d'hôte
            "http://",
            "ceci n'est pas un lien"
    })
    void liensInvalides(String lien) {
        assertThat(ValidateurLien.estValide(lien)).isFalse();
    }

    @ParameterizedTest(name = "refuse le lien vide ou nul")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void lienVide(String lien) {
        assertThat(ValidateurLien.estValide(lien)).isFalse();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("refuse un lien plus long que la colonne qui doit l'accueillir")
    void lienTropLong() {
        // La colonne fait 500 caractères : sans ce contrôle, l'insertion
        // échouerait en base et remonterait en 500 au lieu d'un 400 lisible.
        assertThat(ValidateurLien.estValide("https://exemple.cm/" + "a".repeat(500))).isFalse();
    }
}
