package cm.kfokam48.presence.service;

import cm.kfokam48.presence.domaine.Session;

/**
 * Ce que produit une clôture : la séance fermée, et le compte de ce qui a pu
 * être assigné ou non.
 *
 * <p>{@code exercicesNonAssignes} n'est pas un détail : c'est la trace visible
 * de RG16, le cas où aucun relecteur n'était éligible. Le formateur doit savoir
 * qu'un exercice ne sera jamais noté, et pourquoi.
 */
public record ResultatCloture(Session seance, int relecturesAssignees, int exercicesNonAssignes) {
}
