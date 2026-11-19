package pe.viadeep.evidence.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de S3. En local se apunta al emulador del docker-compose con endpoint y credenciales propias;
 * en AWS, endpoint y credenciales quedan vacíos y el SDK usa el rol de la tarea ECS. publicEndpoint, si se
 * define, es la dirección que llevan las URLs firmadas de las fotos (la que ven el navegador y el celular).
 */
@ConfigurationProperties(prefix = "viadeep.s3")
public record S3Properties(String bucket, String region, String endpoint, String accessKey, String secretKey,
                           String publicEndpoint) {

    boolean usesCustomEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }

    /** Dirección de las URLs de descarga: la pública si existe, si no la misma que usa el servicio. */
    String downloadEndpoint() {
        return publicEndpoint != null && !publicEndpoint.isBlank() ? publicEndpoint.strip() : endpoint;
    }

    boolean usesStaticCredentials() {
        return accessKey != null && !accessKey.isBlank();
    }
}
