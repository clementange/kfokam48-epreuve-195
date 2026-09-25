package cm.kfokam48.presence.repository;

import cm.kfokam48.presence.domaine.Presence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    /** RG3 — une présence est unique par couple (séance, étudiant). */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    List<Presence> findBySessionId(Long sessionId);

    /** RG11, RG15 — le dépôt et le tirage du relecteur se limitent aux présents. */
    List<Presence> findBySessionIdOrderByEtudiantIdAsc(Long sessionId);
}
