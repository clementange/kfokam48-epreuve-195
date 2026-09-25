package cm.kfokam48.presence.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * ENF5 — le code ne doit pas être devinable : Q4 redoute explicitement que les
 * étudiants « devinent les codes entre eux ».
 *
 * <p>{@link SecureRandom} et non {@code Random} : un générateur prévisible
 * rendrait la limitation de tentatives (RG7) inutile.
 *
 * <p>L'alphabet exclut {@code I}, {@code O}, {@code 0} et {@code 1}, qui se
 * confondent quand un étudiant recopie un code projeté au tableau. Il reste
 * 32 symboles, soit environ un milliard de codes possibles.
 */
@Component
public class GenerateurCodeAleatoire implements GenerateurCode {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LONGUEUR = 6;

    private final SecureRandom aleatoire = new SecureRandom();

    @Override
    public String genererCode() {
        StringBuilder code = new StringBuilder(LONGUEUR);
        for (int i = 0; i < LONGUEUR; i++) {
            code.append(ALPHABET.charAt(aleatoire.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
