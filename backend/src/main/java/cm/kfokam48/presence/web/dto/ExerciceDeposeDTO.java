package cm.kfokam48.presence.web.dto;

import cm.kfokam48.presence.domaine.Exercice;
import cm.kfokam48.presence.domaine.StatutExercice;

/** Réponse {@code 201} de {@code POST /api/exercices} : les deux champs du contrat. */
public record ExerciceDeposeDTO(Long id, StatutExercice statut) {

    public static ExerciceDeposeDTO de(Exercice e) {
        return new ExerciceDeposeDTO(e.getId(), e.getStatut());
    }
}
