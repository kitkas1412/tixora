package me.kitkas1412.config;

import java.util.List;
import java.util.Set;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Fails fast with a clear message when required database settings are missing in dev/prod.
 * Only property names are reported, never values.
 */
public class DatabaseEnvironmentCheck implements EnvironmentPostProcessor, Ordered {

    private static final Set<String> CHECKED_PROFILES = Set.of("dev", "prod");
    private static final List<String> REQUIRED = List.of("TIXORA_DB_URL", "TIXORA_DB_USER", "TIXORA_DB_PASSWORD");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String profile = null;
        for (String active : environment.getActiveProfiles()) {
            if (CHECKED_PROFILES.contains(active)) {
                profile = active;
                break;
            }
        }
        if (profile == null) {
            return;
        }
        List<String> missing = REQUIRED.stream()
                .filter(name -> {
                    String value = environment.getProperty(name);
                    return value == null || value.isBlank();
                })
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required database settings for profile '" + profile + "': "
                    + String.join(", ", missing) + ". Set them as environment variables.");
        }
    }

    @Override
    public int getOrder() {
        // run after config data (and thus profile activation) has been processed
        return Ordered.LOWEST_PRECEDENCE;
    }
}
