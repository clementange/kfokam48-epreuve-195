package cm.kfokam48.presence.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autorise le frontend à appeler l'API depuis le navigateur.
 *
 * <p>Le frontend est servi sur un port différent de l'API : toute requête
 * partie de la page est donc <em>cross-origin</em>. Sans ces en-têtes, le
 * navigateur les bloque toutes — et le défaut est sournois, parce qu'il ne se
 * voit pas avec {@code curl} : en ligne de commande tout répond {@code 200},
 * tandis qu'à l'écran l'application est entièrement muette.
 *
 * <p>Les origines sont configurables plutôt que codées en dur : le port du
 * frontend peut changer si 3000 est déjà pris sur la machine.
 */
@Configuration
public class ConfigurationCors implements WebMvcConfigurer {

    private final String[] originesAutorisees;

    public ConfigurationCors(
            @Value("${app.cors.origines:http://localhost:3000}") String origines) {
        this.originesAutorisees = origines.split("\\s*,\\s*");
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registre) {
        registre.addMapping("/api/**")
                .allowedOrigins(originesAutorisees)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // Aucun cookie, aucun en-tête d'authentification : il n'y a pas
                // d'authentification dans ce produit (Q1). Autoriser les
                // identifiants ouvrirait une porte qui ne sert à rien ici.
                .allowCredentials(false)
                .maxAge(3600);
    }
}
