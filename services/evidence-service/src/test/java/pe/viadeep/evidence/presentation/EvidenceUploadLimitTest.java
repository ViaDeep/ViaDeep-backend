package pe.viadeep.evidence.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pe.viadeep.evidence.domain.Evidence;
import pe.viadeep.evidence.domain.ImageFormat;
import pe.viadeep.evidence.domain.InvalidEvidenceException;

class EvidenceUploadLimitTest {

    private static final long FIVE_MEGABYTES = 5_000_000L;

    @Test
    void localAndAwsProfilesUseTheApprovedUploadLimits() throws Exception {
        assertLimits("application.yml");
        assertLimits("application-aws.yml");
    }

    @Test
    void acceptsAPhotoAtTheExactFiveMegabyteLimit() {
        Evidence evidence = Evidence.upload(
                ImageFormat.JPEG, FIVE_MEGABYTES, FIVE_MEGABYTES, UUID.randomUUID(), Instant.EPOCH);

        assertThat(evidence.sizeBytes()).isEqualTo(FIVE_MEGABYTES);
    }

    @Test
    void rejectsAPhotoOneByteOverTheFiveMegabyteLimit() {
        assertThatThrownBy(() -> Evidence.upload(
                ImageFormat.JPEG, FIVE_MEGABYTES + 1, FIVE_MEGABYTES, UUID.randomUUID(), Instant.EPOCH))
                .isInstanceOf(InvalidEvidenceException.class)
                .hasMessageContaining("5000000 bytes");
    }

    @Test
    void multipartOverflowKeepsThePayloadTooLargeContract() {
        ProblemDetail problem = new ApiExceptionHandler()
                .tooLarge(new MaxUploadSizeExceededException(FIVE_MEGABYTES));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(problem.getDetail()).isEqualTo("La fotografía supera el tamaño máximo permitido");
    }

    private static void assertLimits(String resourceName) throws Exception {
        var properties = new YamlPropertySourceLoader()
                .load(resourceName, new ClassPathResource(resourceName))
                .getFirst();

        assertThat(properties.getProperty("spring.servlet.multipart.max-file-size").toString()).isEqualTo("5000000B");
        assertThat(properties.getProperty("spring.servlet.multipart.max-request-size").toString()).isEqualTo("6291456B");
        assertThat(properties.getProperty("viadeep.evidence.max-size-bytes")).isEqualTo(5_000_000);
    }
}
