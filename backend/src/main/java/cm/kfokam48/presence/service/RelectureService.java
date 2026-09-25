package cm.kfokam48.presence.service;

import cm.kfokam48.presence.commun.erreur.CodeErreur;
import cm.kfokam48.presence.commun.erreur.ErreurMetier;
import cm.kfokam48.presence.domaine.*;
import cm.kfokam48.presence.repository.*;
import cm.kfokam48.presence.web.dto.MonExerciceDTO;
import cm.kfokam48.presence.web.dto.RelectureAssigneeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * EF7, EF11 — rendre une relecture, et consulter celle qu'on a reçue.
 *
 * <p><b>Contradiction tranchée (cahier des charges §7).</b> Q10 dit que le
 * relecteur peut corriger sa note « tant que le formateur n'a pas clôturé la
 * session » ; Q15 dit qu'une fois validée, « c'est fini, il ne peut plus y
 * revenir ». Les deux sont incompatibles.
 *
 * <p>Q15 l'emporte, pour trois raisons : le contrat impose
 * {@code 409 RELECTURE_DEJA_RENDUE}, ce qui rendrait Q10 inapplicable et le
 * contrat incohérent ; Q15 est argumentée par le client lui-même — « c'est plus
 * honnête pour tout le monde » — quand Q10 n'est qu'un « oui » sans motif ;
 * enfin, l'assignation ayant lieu à la clôture, la fenêtre de correction offerte
 * par Q10 serait de toute façon vide.
 */
@Service
public class RelectureService {

    private final RelectureRepository relectures;
    private final ExerciceRepository exercices;
    private final SessionRepository sessions;
    private final EtudiantRepository etudiants;
    private final ReferentielService referentiel;
    private final Clock horloge;

    public RelectureService(RelectureRepository relectures,
                            ExerciceRepository exercices,
                            SessionRepository sessions,
                            EtudiantRepository etudiants,
                            ReferentielService referentiel,
                            Clock horloge) {
        this.relectures = relectures;
        this.exercices = exercices;
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.referentiel = referentiel;
        this.horloge = horloge;
    }

    /**
     * EF7 — le relecteur rend sa note et son commentaire.
     *
     * <p>L'ordre des contrôles suit le contrat : note d'abord ({@code 400}), puis
     * auto-relecture ({@code 403}), puis relecture déjà rendue ({@code 409}).
     */
    @Transactional
    public Relecture rendre(Long relectureId, BigDecimal note, String commentaire) {
        if (!Relecture.noteRecevable(note)) {
            throw new ErreurMetier(CodeErreur.NOTE_INVALIDE);
        }

        Relecture relecture = relectures.findById(relectureId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.RELECTURE_INCONNUE));

        Exercice exercice = exercices.findById(relecture.getExerciceId())
                .orElseThrow(() -> new ErreurMetier(CodeErreur.EXERCICE_INCONNU));

        // RG17, Q5 — filet de sécurité. L'assignation garantit déjà que le
        // relecteur n'est pas l'auteur ; on le revérifie ici parce qu'une règle
        // aussi structurante ne doit pas dépendre d'un seul endroit du code.
        if (relecture.getRelecteurId().equals(exercice.getEtudiantId())) {
            throw new ErreurMetier(CodeErreur.AUTO_RELECTURE);
        }

        // RG19 — Q15 l'emporte sur Q10 : une note validée est définitive.
        if (relecture.estRendue()) {
            throw new ErreurMetier(CodeErreur.RELECTURE_DEJA_RENDUE);
        }

        relecture.rendre(note.intValueExact(), commentaire, horloge.instant());
        relectures.saveAndFlush(relecture);

        // Issue #35 — le statut dépend de ce qu'il RESTE à rendre, pas du nombre
        // de notes reçues. Un exercice à un seul relecteur est définitif dès sa
        // première note ; un exercice à deux reste provisoire tant que le second
        // n'a pas répondu.
        long resteARendre = relectures.countByExerciceIdAndStatut(
                exercice.getId(), StatutRelecture.EN_ATTENTE);
        exercice.mettreAJourApresRelecture(resteARendre);
        exercices.save(exercice);

        return relecture;
    }

    /** Écran relecteur : les relectures qui m'ont été assignées. */
    @Transactional(readOnly = true)
    public List<RelectureAssigneeDTO> mesRelectures(Long etudiantId, StatutRelecture filtre) {
        referentiel.exigerEtudiantExistant(etudiantId);

        List<Relecture> miennes = (filtre == null)
                ? relectures.findByRelecteurId(etudiantId)
                : relectures.findByRelecteurIdAndStatut(etudiantId, filtre);

        Map<Long, Exercice> exercicesParId = chargerExercices(
                miennes.stream().map(Relecture::getExerciceId).toList());
        Map<Long, String> titres = titresDesSeances(exercicesParId.values());
        Map<Long, String> auteurs = nomsDesEtudiants(
                exercicesParId.values().stream().map(Exercice::getEtudiantId).toList());

        return miennes.stream().map(r -> {
            Exercice e = exercicesParId.get(r.getExerciceId());
            return new RelectureAssigneeDTO(
                    r.getId(), e.getId(), e.getSessionId(),
                    titres.get(e.getSessionId()),
                    auteurs.get(e.getEtudiantId()),
                    e.getLien(), r.getStatut(), r.getNote(), r.getCommentaire());
        }).toList();
    }

    /**
     * EF11, RG20 — mes exercices et les notes reçues.
     *
     * <p>Aucun champ ne permet d'identifier le relecteur : on ne lit que la note
     * et le commentaire de la relecture, jamais son {@code relecteurId} ni sa
     * date de rendu.
     */
    @Transactional(readOnly = true)
    public List<MonExerciceDTO> mesExercices(Long etudiantId) {
        referentiel.exigerEtudiantExistant(etudiantId);

        List<Exercice> miens = exercices.findByEtudiantIdOrderByDeposeAtDesc(etudiantId);
        Map<Long, String> titres = titresDesSeances(miens);

        // Un exercice a plusieurs relectures depuis l'issue #34 : on les charge
        // toutes en une requête plutôt qu'une par exercice.
        Map<Long, List<Relecture>> parExercice = relectures
                .findByExerciceIdIn(miens.stream().map(Exercice::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(Relecture::getExerciceId));

        return miens.stream().map(e -> {
            List<Relecture> rendues = parExercice.getOrDefault(e.getId(), List.of()).stream()
                    .filter(Relecture::estRendue)
                    .toList();

            // Issue #35 — « la note retenue est la moyenne des deux ». Avec une
            // seule relecture rendue, c'est sa note, marquée provisoire.
            Double note = rendues.isEmpty() ? null
                    : rendues.stream().mapToInt(Relecture::getNote).average().orElseThrow();

            // Les commentaires des deux relecteurs sont présentés ensemble, sans
            // jamais dire qui a écrit quoi : Q8 protège l'anonymat du relecteur.
            String commentaires = rendues.isEmpty() ? null
                    : rendues.stream().map(Relecture::getCommentaire)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("\n\n"));

            return new MonExerciceDTO(
                    e.getId(), e.getSessionId(), titres.get(e.getSessionId()),
                    e.getLien(), e.getStatut(),
                    note == null ? null : arrondir(note),
                    commentaires,
                    e.noteProvisoire(),
                    rendues.size(),
                    parExercice.getOrDefault(e.getId(), List.of()).size());
        }).toList();
    }

    /** Deux décimales, comme le tableau du formateur : la moyenne vient de l'API (F3). */
    private static java.math.BigDecimal arrondir(double valeur) {
        return java.math.BigDecimal.valueOf(valeur)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private Map<Long, Exercice> chargerExercices(List<Long> ids) {
        return exercices.findAllById(ids).stream()
                .collect(Collectors.toMap(Exercice::getId, Function.identity()));
    }

    private Map<Long, String> titresDesSeances(Collection<Exercice> lesExercices) {
        List<Long> ids = lesExercices.stream().map(Exercice::getSessionId).distinct().toList();
        return sessions.findAllById(ids).stream()
                .collect(Collectors.toMap(Session::getId, Session::getTitre));
    }

    private Map<Long, String> nomsDesEtudiants(List<Long> ids) {
        return etudiants.findAllById(ids).stream()
                .collect(Collectors.toMap(Etudiant::getId, Etudiant::getNom, (a, b) -> a));
    }
}
