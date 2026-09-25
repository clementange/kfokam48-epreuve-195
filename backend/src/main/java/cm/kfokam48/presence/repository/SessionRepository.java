package cm.kfokam48.presence.repository;

import cm.kfokam48.presence.domaine.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    boolean existsByCode(String code);

    /**
     * RG4, RG24 — le code n'est cherché que parmi les séances de la promotion de
     * l'étudiant. Un code valide appartenant à une autre promotion doit rester
     * introuvable : le confirmer aiderait à deviner les codes (Q4).
     */
    Optional<Session> findByCodeAndPromotionId(String code, Long promotionId);

    List<Session> findByPromotionIdOrderByOuvertureAtDesc(Long promotionId);
}
