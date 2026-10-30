package pe.viadeep.evidence.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * Solo en local (endpoint propio): crea el bucket de evidencias si no existe. En AWS el bucket
 * lo crea la infraestructura, y este componente no hace nada.
 */
@Component
class LocalBucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalBucketInitializer.class);

    private final S3Client s3;
    private final S3Properties properties;

    LocalBucketInitializer(S3Client s3, S3Properties properties) {
        this.s3 = s3;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.usesCustomEndpoint()) {
            return;
        }
        try {
            s3.headBucket(request -> request.bucket(properties.bucket()));
        } catch (NoSuchBucketException e) {
            s3.createBucket(request -> request.bucket(properties.bucket()));
            log.info("Bucket local {} creado", properties.bucket());
        } catch (SdkException e) {
            log.warn("No se pudo conectar al almacenamiento local en {}: {}", properties.endpoint(), e.getMessage());
        }
    }
}
