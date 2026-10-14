package pe.viadeep.auth.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del JWT. En AWS, la clave privada (PKCS#8, PEM) llega desde Secrets Manager
 * en VIADEEP_JWT_PRIVATE_KEY. Si está vacía, se genera un par de claves temporal (solo desarrollo).
 */
@ConfigurationProperties(prefix = "viadeep.jwt")
public record JwtProperties(String issuer, Duration ttl, String keyId, String privateKey) {
}
