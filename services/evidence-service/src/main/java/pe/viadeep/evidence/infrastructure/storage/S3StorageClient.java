package pe.viadeep.evidence.infrastructure.storage;

import java.net.URL;
import java.time.Duration;
import org.springframework.stereotype.Component;
import pe.viadeep.evidence.application.EvidenceStorage;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
class S3StorageClient implements EvidenceStorage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    S3StorageClient(S3Client s3, S3Presigner presigner, S3Properties properties) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = properties.bucket();
    }

    @Override
    public void store(String objectKey, byte[] content, String contentType) {
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(objectKey).contentType(contentType).build(),
                RequestBody.fromBytes(content));
    }

    @Override
    public URL presignedDownloadUrl(String objectKey, Duration ttl) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(objectKey).build())
                .build();
        return presigner.presignGetObject(request).url();
    }
}
