package cm.kfokam48.presence.service;

import java.util.List;

/**
 * RG15, Q7 — « Le système, au hasard, parmi les étudiants présents à cette
 * session. »
 *
 * <p>Le tirage est isolé derrière une interface pour la même raison que le
 * générateur de code : un comportement aléatoire ne se teste pas. Les tests
 * substituent un tirage déterministe et vérifient la règle, pas la chance.
 */
public interface TirageRelecteur {

    /**
     * @param candidats étudiants éligibles, jamais l'auteur de l'exercice
     * @return l'un d'eux, ou {@code null} si la liste est vide (RG16)
     */
    Long tirer(List<Long> candidats);
}
