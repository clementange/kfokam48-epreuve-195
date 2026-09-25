package cm.kfokam48.presence.repository;

import cm.kfokam48.presence.domaine.Relecture;
import cm.kfokam48.presence.domaine.StatutRelecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    Optional<Relecture> findByExerciceId(Long exerciceId);

    List<Relecture> findByRelecteurId(Long relecteurId);

    List<Relecture> findByRelecteurIdAndStatut(Long relecteurId, StatutRelecture statut);
}
