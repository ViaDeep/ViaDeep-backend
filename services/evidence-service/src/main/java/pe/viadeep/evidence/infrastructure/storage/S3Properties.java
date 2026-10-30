package pe.viadeep.evidence.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de S3. En local se apunta al emulador del docker-compose con endpoint y credenciales propias;
 * en AWS, endpoint y credenciales quedan vacíos y el SDK usa el rol de la tarea ECS.
 */
@ConfigurationProperties(prefix = "viadeep.s3")
public record S3Properties(String bucket, String region, String endpoint, String accessKey, String secretKey) {

    boolean usesCustomEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }

    boolean usesStaticCredentials() {
        return accessKey != null && !accessKey.isBlank();
    }
}
