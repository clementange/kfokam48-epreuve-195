package cm.kfokam48.presence.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** ENF5 — le code ne doit être ni devinable, ni ambigu à la lecture. */
class GenerateurCodeAleatoireTest {

    private final GenerateurCodeAleatoire generateur = new GenerateurCodeAleatoire();

    @RepeatedTest(50)
    @DisplayName("ENF5 — 6 caractères, tous pris dans l'alphabet sans caractères ambigus")
    void formatDuCode() {
        String code = generateur.genererCode();

        assertThat(code).hasSize(6);
        assertThat(code.chars())
                .allMatch(c -> GenerateurCodeAleatoire.ALPHABET.indexOf(c) >= 0);
    }

    @Test
    @DisplayName("ENF5 — I, O, 0 et 1 sont exclus : ils se confondent quand on recopie un code")
    void alphabetSansCaracteresAmbigus() {
        assertThat(GenerateurCodeAleatoire.ALPHABET)
                .doesNotContain("I")
                .doesNotContain("O")
                .doesNotContain("0")
                .doesNotContain("1")
                .hasSize(32);
    }

    @Test
    @DisplayName("les codes tirés ne se répètent pas en pratique")
    void codesDistincts() {
        Set<String> codes = new HashSet<>();
        IntStream.range(0, 1_000).forEach(i -> codes.add(generateur.genererCode()));

        // 1 000 tirages sur 32^6 possibilités : une collision signalerait un
        // générateur qui ne tire pas uniformément.
        assertThat(codes).hasSize(1_000);
    }
}
