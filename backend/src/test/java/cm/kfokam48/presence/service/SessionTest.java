package cm.kfokam48.presence.service;

import cm.kfokam48.presence.domaine.Session;
import cm.kfokam48.presence.domaine.StatutSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire de la règle RG1 — « le code de présence expire 15 minutes après
 * l'ouverture de la séance » (Q2).
 *
 * <p>Aucun contexte Spring, aucune base : la règle est dans l'entité, on la
 * teste là où elle est. L'instant est injecté, ce qui permet de vérifier les
 * deux côtés de la limite sans attendre un quart d'heure.
 */
class SessionTest {

    private static final Instant OUVERTURE = Instant.parse("2026-09-25T08:30:00Z");

    private Session seanceOuverteA(Instant ouverture) {
        return Session.ouvrir("Séance 12 — Spring Data JPA", 1L, "K7M2QX", ouverture);
    }

    @Test
    @DisplayName("RG1 — l'expiration est fixée exactement 15 minutes après l'ouverture")
    void expirationQuinzeMinutesApresOuverture() {
        Session seance = seanceOuverteA(OUVERTURE);

        assertThat(Duration.between(seance.getOuvertureAt(), seance.getExpirationAt()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("RG1 — à 14 minutes 59, le code vaut encore")
    void codeValideJusteAvantLaLimite() {
        Session seance = seanceOuverteA(OUVERTURE);

        assertThat(seance.codeExpire(OUVERTURE.plus(Duration.ofSeconds(899)))).isFalse();
    }

    @Test
    @DisplayName("RG1 — à 15 minutes pile, le code est expiré : la limite est exclusive")
    void codeExpireALaLimiteExacte() {
        Session seance = seanceOuverteA(OUVERTURE);

        assertThat(seance.codeExpire(OUVERTURE.plus(Duration.ofMinutes(15)))).isTrue();
    }

    @Test
    @DisplayName("RG1 — bien après, le code reste expiré")
    void codeExpireLongtempsApres() {
        Session seance = seanceOuverteA(OUVERTURE);

        assertThat(seance.codeExpire(OUVERTURE.plus(Duration.ofHours(3)))).isTrue();
    }

    @Test
    @DisplayName("une séance naît OUVERTE, sans date de clôture")
    void seanceNaitOuverte() {
        Session seance = seanceOuverteA(OUVERTURE);

        assertThat(seance.getStatut()).isEqualTo(StatutSession.OUVERTE);
        assertThat(seance.estCloturee()).isFalse();
        assertThat(seance.getClotureAt()).isNull();
    }

    @Test
    @DisplayName("RG14 — la clôture fige le statut et horodate")
    void clotureFigeLeStatut() {
        Session seance = seanceOuverteA(OUVERTURE);
        Instant cloture = OUVERTURE.plus(Duration.ofHours(2));

        seance.cloturer(cloture);

        assertThat(seance.estCloturee()).isTrue();
        assertThat(seance.getStatut()).isEqualTo(StatutSession.CLOTUREE);
        assertThat(seance.getClotureAt()).isEqualTo(cloture);
    }

    @Test
    @DisplayName("l'expiration du code et la clôture sont deux notions distinctes")
    void expirationEtClotureSontIndependantes() {
        Session seance = seanceOuverteA(OUVERTURE);
        Instant bienApresExpiration = OUVERTURE.plus(Duration.ofHours(5));

        // C'est la distinction que la demande du client ne faisait pas, et qui
        // rend Q12 applicable : le dépôt reste ouvert alors que le code est mort.
        assertThat(seance.codeExpire(bienApresExpiration)).isTrue();
        assertThat(seance.estCloturee()).isFalse();
    }
}
