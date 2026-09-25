package cm.kfokam48.presence.service;

import cm.kfokam48.presence.domaine.Session;

/**
 * Ce que produit une clôture : la séance fermée, et le compte de ce qui a pu
 * être assigné.
 *
 * <p>Les trois compteurs disent au formateur, au moment où il clôture, ce qui
 * pourra être noté et ce qui ne le sera pas. Depuis l'issue #34, un exercice
 * peut recevoir deux relecteurs, un seul, ou aucun — et ces trois situations
 * n'ont pas les mêmes conséquences pour l'étudiant.
 *
 * @param relecturesAssignees   nombre total de relectures créées
 * @param exercicesNonAssignes  aucun pair éligible : l'exercice ne sera jamais noté (RG16)
 * @param exercicesUnSeulRelecteur un seul pair éligible : la note sera définitive dès
 *                                 la première relecture, sans moyenne possible
 */
public record ResultatCloture(
        Session seance,
        int relecturesAssignees,
        int exercicesNonAssignes,
        int exercicesUnSeulRelecteur
) {
}
