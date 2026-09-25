package cm.kfokam48.presence.service;

import cm.kfokam48.presence.domaine.Relecture;
import cm.kfokam48.presence.domaine.StatutRelecture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire de RG18 — « Sur 20, en nombres entiers » (Q9) — et de RG19,
 * la note définitive.
 *
 * <p>Aucun contexte Spring : ce sont des règles, elles se testent là où elles
 * vivent. Les bornes sont vérifiées des deux côtés, y compris exactement sur
 * la limite : c'est là que les erreurs se cachent.
 */
class RelectureNoteTest {

    @ParameterizedTest(name = "RG18 — {0} est une note recevable")
    @ValueSource(ints = {0, 1, 10, 19, 20})
    void notesRecevables(int note) {
        assertThat(Relecture.noteRecevable(note)).isTrue();
    }

    @ParameterizedTest(name = "RG18 — {0} est refusée")
    @ValueSource(ints = {-1, -20, 21, 100, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void notesHorsBornes(int note) {
        assertThat(Relecture.noteRecevable(note)).isFalse();
    }

    @ParameterizedTest(name = "RG18 — une note absente est refusée")
    @NullSource
    void noteAbsente(Integer note) {
        assertThat(Relecture.noteRecevable(note)).isFalse();
    }

    @Test
    @DisplayName("RG18 — les deux bornes exactes sont incluses")
    void bornesIncluses() {
        assertThat(Relecture.noteRecevable(Relecture.NOTE_MINIMALE)).isTrue();
        assertThat(Relecture.noteRecevable(Relecture.NOTE_MAXIMALE)).isTrue();
        assertThat(Relecture.noteRecevable(Relecture.NOTE_MINIMALE - 1)).isFalse();
        assertThat(Relecture.noteRecevable(Relecture.NOTE_MAXIMALE + 1)).isFalse();
    }

    @Test
    @DisplayName("une relecture naît EN_ATTENTE, sans note ni date de rendu")
    void relectureNaitEnAttente() {
        Relecture r = Relecture.assigner(1L, 2L, Instant.parse("2026-09-25T12:00:00Z"));

        assertThat(r.getStatut()).isEqualTo(StatutRelecture.EN_ATTENTE);
        assertThat(r.estRendue()).isFalse();
        assertThat(r.getNote()).isNull();
        assertThat(r.getRendueAt()).isNull();
    }

    @Test
    @DisplayName("RG19 — rendre fige la note, le commentaire et la date")
    void rendreFigeLaNote() {
        Relecture r = Relecture.assigner(1L, 2L, Instant.parse("2026-09-25T12:00:00Z"));
        Instant rendu = Instant.parse("2026-09-25T14:30:00Z");

        r.rendre(16, "Bonne structure, il manque la gestion des erreurs.", rendu);

        assertThat(r.estRendue()).isTrue();
        assertThat(r.getStatut()).isEqualTo(StatutRelecture.RENDUE);
        assertThat(r.getNote()).isEqualTo(16);
        assertThat(r.getCommentaire()).isEqualTo("Bonne structure, il manque la gestion des erreurs.");
        assertThat(r.getRendueAt()).isEqualTo(rendu);
    }
}
