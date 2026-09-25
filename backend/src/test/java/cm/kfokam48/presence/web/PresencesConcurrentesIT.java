package cm.kfokam48.presence.web;

import cm.kfokam48.presence.commun.erreur.CodeErreur;
import cm.kfokam48.presence.commun.erreur.ErreurMetier;
import cm.kfokam48.presence.service.PresenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #31 — deux étudiants qui saisissent le code en même temps.
 *
 * <p>Ce test reproduit le signalement du client : « ils ont tapé le code presque
 * en même temps et il n'y en a qu'un seul qui apparaît ».
 *
 * <p><b>Pas de {@code @Transactional} ici</b>, contrairement aux autres tests
 * d'intégration, et c'est essentiel : les fils d'exécution doivent ouvrir des
 * transactions réellement distinctes. Une transaction de test partagée
 * sérialiserait les accès et masquerait précisément ce qu'on cherche à
 * démontrer. Le nettoyage est donc fait à la main.
 *
 * <p>Un {@link CyclicBarrier} synchronise le départ : sans lui, les requêtes
 * s'exécuteraient l'une après l'autre et le test ne prouverait rien.
 */
@SpringBootTest
class PresencesConcurrentesIT {

    private static final Long PROMOTION = 1L;
    /** Douze étudiants de la promotion 1, comme une vraie promotion en début de séance. */
    private static final List<Long> ETUDIANTS = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);

    @Autowired private PresenceService service;
    @Autowired private JdbcTemplate jdbc;

    private Long seanceId;
    private String code;

    @BeforeEach
    void ouvrirUneSeance() {
        code = "CNC" + (System.nanoTime() % 1000);
        Instant t = Instant.now();
        jdbc.update("""
                INSERT INTO session (titre, promotion_id, code, ouverture_at, expiration_at, statut)
                VALUES (?, ?, ?, ?, ?, 'OUVERTE')
                """, "Séance concurrente", PROMOTION, code,
                java.sql.Timestamp.from(t), java.sql.Timestamp.from(t.plus(15, ChronoUnit.MINUTES)));
        seanceId = jdbc.queryForObject("SELECT id FROM session WHERE code = ?", Long.class, code);
    }

    @AfterEach
    void nettoyer() {
        jdbc.update("DELETE FROM presence WHERE session_id = ?", seanceId);
        jdbc.update("DELETE FROM session WHERE id = ?", seanceId);
    }

    /** Résultat d'un marquage : soit un succès, soit le code d'erreur reçu. */
    private record Issue(boolean succes, String codeErreur) {}

    /** Lance en parallèle un marquage par étudiant, tous démarrant au même instant. */
    private List<Issue> marquerEnParallele(List<Long> etudiants) throws Exception {
        int n = etudiants.size();
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CyclicBarrier depart = new CyclicBarrier(n);
        List<Future<Issue>> futurs = new ArrayList<>();

        try {
            for (Long etudiantId : etudiants) {
                futurs.add(pool.submit(() -> {
                    depart.await(10, TimeUnit.SECONDS);
                    try {
                        service.marquer(code, etudiantId);
                        return new Issue(true, null);
                    } catch (ErreurMetier e) {
                        return new Issue(false, e.code().name());
                    } catch (Exception e) {
                        return new Issue(false, e.getClass().getSimpleName());
                    }
                }));
            }
            List<Issue> issues = new ArrayList<>();
            for (Future<Issue> f : futurs) {
                issues.add(f.get(20, TimeUnit.SECONDS));
            }
            return issues;
        } finally {
            pool.shutdownNow();
        }
    }

    private int presencesEnBase() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM presence WHERE session_id = ?", Integer.class, seanceId);
        return n == null ? 0 : n;
    }

    @Test
    @DisplayName("Issue #31 — douze étudiants distincts marquent en même temps : les douze doivent passer")
    void douzeEtudiantsDistinctsSimultanes() throws Exception {
        List<Issue> issues = marquerEnParallele(ETUDIANTS);

        List<String> echecs = issues.stream()
                .filter(i -> !i.succes())
                .map(Issue::codeErreur)
                .toList();

        assertThat(echecs)
                .as("aucun étudiant ne doit être refusé : ils sont tous différents, "
                        + "aucune règle ne s'y oppose. Codes reçus : %s", echecs)
                .isEmpty();

        assertThat(presencesEnBase())
                .as("chaque étudiant présent doit avoir sa ligne")
                .isEqualTo(ETUDIANTS.size());
    }

    @Test
    @DisplayName("RG3 — le même étudiant en double simultané : une seule présence, les autres en 409")
    void memeEtudiantSimultane() throws Exception {
        // La correction ne doit pas affaiblir RG3 : l'unicité reste garantie,
        // y compris quand la vérification applicative est franchie par plusieurs
        // requêtes à la fois.
        List<Long> memeEtudiantHuitFois = List.of(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L);

        List<Issue> issues = marquerEnParallele(memeEtudiantHuitFois);

        long succes = issues.stream().filter(Issue::succes).count();
        assertThat(succes).as("une seule présence doit être acceptée").isEqualTo(1);

        assertThat(issues.stream().filter(i -> !i.succes()).map(Issue::codeErreur))
                .as("les autres doivent recevoir le code du contrat, pas une erreur technique")
                .containsOnly(CodeErreur.DEJA_PRESENT.name());

        assertThat(presencesEnBase()).isEqualTo(1);
    }

    @Test
    @DisplayName("un mélange réaliste : dix étudiants dont trois qui double-cliquent")
    void melangeRealiste() throws Exception {
        List<Long> avecDoublons = new ArrayList<>(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
        avecDoublons.addAll(List.of(1L, 5L, 9L)); // trois double-clics

        List<Issue> issues = marquerEnParallele(avecDoublons);

        AtomicInteger dejaPresent = new AtomicInteger();
        List<String> autresErreurs = new ArrayList<>();
        issues.stream().filter(i -> !i.succes()).forEach(i -> {
            if (CodeErreur.DEJA_PRESENT.name().equals(i.codeErreur())) dejaPresent.incrementAndGet();
            else autresErreurs.add(i.codeErreur());
        });

        assertThat(autresErreurs)
                .as("les seuls refus légitimes sont les doublons. Autres erreurs : %s", autresErreurs)
                .isEmpty();
        assertThat(dejaPresent.get()).isEqualTo(3);
        assertThat(presencesEnBase()).isEqualTo(10);
    }
}
