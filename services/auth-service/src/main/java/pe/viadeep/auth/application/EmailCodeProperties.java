package pe.viadeep.auth.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Vigencia de los códigos enviados por correo, tiempo mínimo entre dos envíos al mismo correo y cuántos intentos
 * fallidos se aceptan antes de pedir un código nuevo.
 */
@ConfigurationProperties(prefix = "viadeep.email-codes")
public record EmailCodeProperties(Duration ttl, Duration resendCooldown, Integer maxAttempts) {

    public EmailCodeProperties {
        ttl = ttl == null ? Duration.ofMinutes(15) : ttl;
        resendCooldown = resendCooldown == null ? Duration.ofSeconds(60) : resendCooldown;
        maxAttempts = maxAttempts == null ? 5 : maxAttempts;
    }
}
