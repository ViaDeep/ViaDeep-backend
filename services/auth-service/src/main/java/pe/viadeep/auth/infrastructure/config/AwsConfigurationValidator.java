package pe.viadeep.auth.infrastructure.config;

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
                "VIADEEP_JWT_ISSUER", "VIADEEP_JWT_TTL", "VIADEEP_JWT_KEY_ID", "VIADEEP_JWT_PRIVATE_KEY",
                "OPERATOR_EMAIL_DOMAINS", "MAIL_HOST", "MAIL_PORT", "MAIL_USERNAME", "MAIL_PASSWORD", "MAIL_FROM");
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
