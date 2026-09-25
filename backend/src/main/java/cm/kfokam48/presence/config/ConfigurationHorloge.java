package cm.kfokam48.presence.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * L'heure est une dépendance comme une autre.
 *
 * <p>Sans cette horloge injectable, RG1 — « le code expire 15 minutes après
 * l'ouverture » — ne serait testable qu'en attendant un quart d'heure. Les tests
 * substituent une horloge fixe et vérifient les deux côtés de la limite.
 *
 * <p>UTC partout, conformément au §8 du cahier des charges.
 */
@Configuration
public class ConfigurationHorloge {

    @Bean
    public Clock horloge() {
        return Clock.systemUTC();
    }
}
