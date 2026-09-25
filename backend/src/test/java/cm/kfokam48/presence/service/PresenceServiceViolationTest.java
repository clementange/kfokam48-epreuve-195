package cm.kfokam48.presence.service;

import cm.kfokam48.presence.commun.erreur.CodeErreur;
import cm.kfokam48.presence.commun.erreur.ErreurMetier;
import cm.kfokam48.presence.domaine.Etudiant;
import cm.kfokam48.presence.domaine.Presence;
import cm.kfokam48.presence.domaine.Session;
import cm.kfokam48.presence.repository.PresenceRepository;
import cm.kfokam48.presence.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Issue #31 — ce que l'investigation a réellement mis au jour.
 *
 * <p>Le symptôme signalé par le client — deux étudiants simultanés, un seul
 * enregistré — ne se reproduit pas : la contrainte {@code uq_presence_session_etudiant}
 * est en base depuis le premier jour et deux étudiants <em>différents</em> ne
 * peuvent pas entrer en conflit.
 *
 * <p>En revanche, la zone de code incriminée contient un vrai défaut :
 * {@code catch (DataIntegrityViolationException)} attrape <b>toute</b> violation
 * d'intégrité et la traduit en {@code DEJA_PRESENT}. Une violation de clé
 * étrangère, de {@code CHECK} ou de {@code NOT NULL} — pour l'instant
 * inatteignable par l'API, mais qu'une évolution peut rendre atteignable —
 * répondrait donc « Votre présence est déjà enregistrée » à un étudiant qui
 * n'était pas présent.
 *
 * <p>C'est exactement le message que le client a vu. Le défaut est réel, même si
 * le chemin pour l'atteindre n'est pas celui qu'il décrit.
 */
class PresenceServiceViolationTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-25T08:35:00Z");
    private static final String CODE = "K7M2QX";
    private static final Long ETUDIANT = 7L;

    private PresenceRepository presences;
    private PresenceService service;

    @BeforeEach
    void preparer() {
        SessionRepository sessions = mock(SessionRepository.class);
        presences = mock(PresenceRepository.class);
        ReferentielService referentiel = mock(ReferentielService.class);

        Etudiant etudiant = mock(Etudiant.class);
        when(etudiant.getPromotionId()).thenReturn(1L);
        when(referentiel.exigerEtudiantExistant(ETUDIANT)).thenReturn(etudiant);

        Session seance = Session.ouvrir("Séance 12", 1L, CODE,
                MAINTENANT.minusSeconds(120));
        when(sessions.findByCodeAndPromotionId(CODE, 1L)).thenReturn(Optional.of(seance));
        when(presences.existsBySessionIdAndEtudiantId(any(), anyLong())).thenReturn(false);

        service = new PresenceService(sessions, presences, referentiel,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("RG3 — une violation de l'unicité (session, étudiant) répond bien DEJA_PRESENT")
    void violationDUnicite() {
        when(presences.saveAndFlush(any(Presence.class))).thenThrow(
                new DataIntegrityViolationException(
                        "could not execute statement [ERROR: duplicate key value violates unique "
                                + "constraint \"uq_presence_session_etudiant\"]"));

        assertThatThrownBy(() -> service.marquer(CODE, ETUDIANT))
                .isInstanceOf(ErreurMetier.class)
                .satisfies(e -> assertThat(((ErreurMetier) e).code())
                        .isEqualTo(CodeErreur.DEJA_PRESENT));
    }

    @Test
    @DisplayName("Issue #31 — une violation d'une AUTRE contrainte ne doit pas dire « déjà présent »")
    void violationDUneAutreContrainte() {
        // Une clé étrangère cassée n'a rien à voir avec une présence en double.
        // Répondre DEJA_PRESENT envoie l'étudiant chercher un problème qui
        // n'existe pas, et masque au formateur un défaut d'intégrité réel.
        when(presences.saveAndFlush(any(Presence.class))).thenThrow(
                new DataIntegrityViolationException(
                        "could not execute statement [ERROR: insert or update on table \"presence\" "
                                + "violates foreign key constraint \"fk_presence_session\"]"));

        assertThatThrownBy(() -> service.marquer(CODE, ETUDIANT))
                .isInstanceOf(ErreurMetier.class)
                .satisfies(e -> assertThat(((ErreurMetier) e).code())
                        .as("le service ne doit pas prétendre que l'étudiant était déjà présent")
                        .isNotEqualTo(CodeErreur.DEJA_PRESENT));
    }

    @Test
    @DisplayName("Issue #31 — une violation de CHECK ne doit pas non plus dire « déjà présent »")
    void violationDUnCheck() {
        when(presences.saveAndFlush(any(Presence.class))).thenThrow(
                new DataIntegrityViolationException(
                        "could not execute statement [ERROR: new row for relation \"presence\" "
                                + "violates check constraint \"ck_presence_source\"]"));

        assertThatThrownBy(() -> service.marquer(CODE, ETUDIANT))
                .isInstanceOf(ErreurMetier.class)
                .satisfies(e -> assertThat(((ErreurMetier) e).code())
                        .isNotEqualTo(CodeErreur.DEJA_PRESENT));
    }
}
