package pe.viadeep.evidence.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class S3PropertiesTest {

    @Test
    void downloadUrlsUseThePublicEndpointWhenItIsSet() {
        var server = new S3Properties("bucket", "us-east-1", "http://storage:9090", "key", "secret",
                "https://viadeep.example.com:9443");
        assertThat(server.downloadEndpoint()).isEqualTo("https://viadeep.example.com:9443");

        var local = new S3Properties("bucket", "us-east-1", "http://192.168.1.40:9000", "key", "secret", " ");
        assertThat(local.downloadEndpoint()).isEqualTo("http://192.168.1.40:9000");
    }

    @Test
    void presignedUrlPointsToThePublicEndpointWhileUploadsStayInternal() {
        var properties = new S3Properties("viadeep-evidences", "us-east-1", "http://storage:9090", "key", "secret",
                "https://viadeep.example.com:9443");
        var config = new S3Config();
        var client = new S3StorageClient(config.s3Client(properties), config.s3Presigner(properties), properties);

        String url = client.presignedDownloadUrl("photos/a.jpg", Duration.ofMinutes(10)).toString();

        assertThat(url).startsWith("https://viadeep.example.com:9443/viadeep-evidences/photos/a.jpg?");
    }
}
