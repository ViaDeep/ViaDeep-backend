package pe.viadeep.notification.infrastructure.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Impide que el perfil aws arranque con secretos o destinos críticos ausentes o vacíos. */
@Configuration
@Profile("aws")
class AwsConfigurationValidator {

    AwsConfigurationValidator(Environment environment) {
        require(environment, "DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD", "DB_SSL_ROOT_CERT",
                "AUTH_JWKS_URI", "VIADEEP_JWT_ISSUER",
                "NOTIFICATIONS_INTERNAL_TOKEN");
    }

    private static void require(Environment environment, String... names) {
        List<String> missing = Arrays.stream(names)
                .filter(name -> {
                    String value = environment.getProperty(name);
                    return value == null || value.isBlank();
                })
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Configuración aws incompleta. Faltan variables obligatorias: "
                    + String.join(", ", missing));
        }
    }
}
