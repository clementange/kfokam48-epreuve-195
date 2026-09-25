package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.repository.LigneTableau;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Une ligne du tableau du formateur — les six champs imposés par le contrat.
 *
 * <p>La moyenne est arrondie à deux décimales ici, au plus près de la sortie :
 * 13.333333333 n'apporte rien à un formateur. L'arrondi est fait par l'API et
 * non par le frontend, parce que F3 interdit de dupliquer une règle métier côté
 * client — le contrat dit que la moyenne vient de l'API, arrondi compris.
 */
public record LigneTableauDTO(
        Long etudiantId,
        String nom,
        long presences,
        long exercicesDeposes,
        BigDecimal moyenne,
        long relecturesEnAttente
) {
    public static LigneTableauDTO de(LigneTableau l) {
        return new LigneTableauDTO(
                l.getEtudiantId(),
                l.getNom(),
                l.getPresences(),
                l.getExercicesDeposes(),
                // RG22 — null reste null. Un 0 afficherait « 0/20 » à un étudiant
                // qui n'a simplement pas encore été relu.
                l.getMoyenne() == null
                        ? null
                        : BigDecimal.valueOf(l.getMoyenne()).setScale(2, RoundingMode.HALF_UP),
                l.getRelecturesEnAttente());
    }
}
