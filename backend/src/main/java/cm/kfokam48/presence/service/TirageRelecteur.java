package cm.kfokam48.presence.service;

import java.util.List;

/**
 * RG15 révisée — « chaque exercice est relu par deux pairs différents »
 * (changement de besoin de l'étape 3, issue #34). Q7 reste valable pour le
 * reste : « le système, au hasard, parmi les étudiants présents à cette session ».
 *
 * <p>Le tirage est isolé derrière une interface pour la même raison que le
 * générateur de code : un comportement aléatoire ne se teste pas. Les tests
 * substituent un tirage déterministe et vérifient la règle, pas la chance.
 */
public interface TirageRelecteur {

    /**
     * Tire jusqu'à {@code combien} relecteurs distincts parmi les candidats.
     *
     * @param candidats étudiants éligibles, l'auteur de l'exercice déjà exclu
     * @param combien   nombre souhaité — deux depuis l'issue #34
     * @return entre 0 et {@code combien} relecteurs, tous différents. Une liste
     *         plus courte que demandé signifie qu'il n'y avait pas assez de
     *         pairs éligibles : le cas est normal et doit être traité, pas
     *         considéré comme une erreur (RG16)
     */
    List<Long> tirer(List<Long> candidats, int combien);
}
