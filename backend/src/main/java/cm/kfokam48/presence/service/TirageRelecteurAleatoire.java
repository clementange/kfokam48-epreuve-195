package cm.kfokam48.presence.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;
import java.util.random.RandomGenerator;

/** Implémentation de production : tirage uniforme parmi les candidats. */
@Component
public class TirageRelecteurAleatoire implements TirageRelecteur {

    private final RandomGenerator aleatoire = new SecureRandom();

    @Override
    public Long tirer(List<Long> candidats) {
        if (candidats == null || candidats.isEmpty()) {
            return null;
        }
        return candidats.get(aleatoire.nextInt(candidats.size()));
    }
}
