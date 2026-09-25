package cm.kfokam48.presence.repository;

import cm.kfokam48.presence.domaine.Relecture;
import cm.kfokam48.presence.domaine.StatutRelecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    /**
     * Un exercice a plusieurs relectures depuis l'issue #34. Cette méthode
     * renvoyait un {@code Optional} tant qu'il n'y en avait qu'une.
     */
    List<Relecture> findByExerciceId(Long exerciceId);

    List<Relecture> findByExerciceIdIn(List<Long> exerciceIds);

    long countByExerciceIdAndStatut(Long exerciceId, StatutRelecture statut);

    List<Relecture> findByRelecteurId(Long relecteurId);

    List<Relecture> findByRelecteurIdAndStatut(Long relecteurId, StatutRelecture statut);
}
