package cm.kfokam48.presence.repository;

import cm.kfokam48.presence.domaine.Exercice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    /** RG9 — un seul exercice par couple (séance, étudiant). */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    List<Exercice> findBySessionId(Long sessionId);

    List<Exercice> findByEtudiantIdOrderByDeposeAtDesc(Long etudiantId);
}
