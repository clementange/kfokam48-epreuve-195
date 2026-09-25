package cm.kfokam48.presence.repository;

import cm.kfokam48.presence.domaine.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    List<Etudiant> findByPromotionIdOrderByNomAsc(Long promotionId);

    /**
     * EF8, Q16 — le tableau du formateur, en <b>une seule requête</b>.
     *
     * <p>Quatre agrégats par étudiant, calculés en base plutôt qu'en Java : avec
     * 60 étudiants et 30 séances, une boucle applicative ferait 240 requêtes là
     * où celle-ci en fait une (ENF2).
     *
     * <p>Trois points de sémantique, chacun venant d'une règle :
     * <ul>
     *   <li><b>RG22</b> — {@code AVG} renvoie {@code NULL} quand il n'y a aucune
     *       note, et c'est exactement ce que le contrat demande : {@code moyenne}
     *       est {@code nullable}. Un {@code COALESCE(…, 0)} afficherait 0/20 à un
     *       étudiant qui n'a simplement pas encore été relu.</li>
     *   <li>La moyenne ne porte que sur les relectures {@code RENDUE} : une
     *       relecture en attente n'a pas de note, elle ne doit pas peser.</li>
     *   <li><b>RG21</b> — {@code relectures_en_attente} compte les relectures dont
     *       l'étudiant est le <b>relecteur</b>, pas celles qu'il attend en tant
     *       qu'auteur. C'est ce que Q16 demande : « les relectures qu'il doit
     *       encore faire ».</li>
     * </ul>
     *
     * <p>SQL natif volontairement portable, sans fonction propriétaire : la même
     * requête s'exécute sur H2 pendant les tests et sur PostgreSQL en production.
     */
    @Query(value = """
            SELECT e.id   AS etudiantId,
                   e.nom  AS nom,
                   (SELECT COUNT(*) FROM presence p
                     WHERE p.etudiant_id = e.id)                       AS presences,
                   (SELECT COUNT(*) FROM exercice x
                     WHERE x.etudiant_id = e.id)                       AS exercicesDeposes,
                   (SELECT AVG(r.note * 1.0)
                      FROM relecture r
                      JOIN exercice xa ON xa.id = r.exercice_id
                     WHERE xa.etudiant_id = e.id
                       AND r.statut = 'RENDUE')                        AS moyenne,
                   (SELECT COUNT(*) FROM relecture rr
                     WHERE rr.relecteur_id = e.id
                       AND rr.statut = 'EN_ATTENTE')                   AS relecturesEnAttente
              FROM etudiant e
             WHERE e.promotion_id = :promotionId
             ORDER BY e.nom
            """, nativeQuery = true)
    List<LigneTableau> tableauDeLaPromotion(@Param("promotionId") Long promotionId);
}
