package cm.kfokam48.presence.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/** Implémentation de production : tirage uniforme et sans remise. */
@Component
public class TirageRelecteurAleatoire implements TirageRelecteur {

    private final RandomGenerator aleatoire = new SecureRandom();

    @Override
    public List<Long> tirer(List<Long> candidats, int combien) {
        if (candidats == null || candidats.isEmpty() || combien <= 0) {
            return List.of();
        }
        // Sans remise : mélanger puis prendre les premiers garantit que les
        // relecteurs tirés sont distincts, sans boucle de rejet.
        List<Long> melanges = new ArrayList<>(candidats);
        Collections.shuffle(melanges, aleatoire);
        return List.copyOf(melanges.subList(0, Math.min(combien, melanges.size())));
    }
}
