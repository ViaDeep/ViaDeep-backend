package pe.viadeep.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pe.viadeep.evidence.application.EvidenceStorage;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvidenceApiTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private InMemoryStorage storage;

    @Test
    void citizenUploadsPhotoAndGetsItsMetadataAndSignedUrl() throws Exception {
        UUID citizen = UUID.randomUUID();
        String id = upload(citizen, JPEG);

        mvc.perform(get("/evidences/" + id).with(user(citizen, "CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType", is("image/jpeg")))
                .andExpect(jsonPath("$.sizeBytes", is(JPEG.length)))
                .andExpect(jsonPath("$.uploadedBy", is(citizen.toString())));

        mvc.perform(get("/evidences/" + id + "/url").with(user(citizen, "CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", startsWith("https://signed.test/evidences/" + citizen)));

        assertThat(storage.objects.values()).anySatisfy(content -> assertThat(content).isEqualTo(JPEG));
    }

    @Test
    void operatorCanSeeAnyEvidenceButAnotherCitizenCannot() throws Exception {
        String id = upload(UUID.randomUUID(), JPEG);

        mvc.perform(get("/evidences/" + id).with(user(UUID.randomUUID(), "OPERATOR"))).andExpect(status().isOk());
        mvc.perform(get("/evidences/" + id).with(user(UUID.randomUUID(), "CITIZEN"))).andExpect(status().isForbidden());
        mvc.perform(get("/evidences/" + id + "/url").with(user(UUID.randomUUID(), "CITIZEN"))).andExpect(status().isForbidden());
    }

    @Test
    void rejectsFilesThatAreNotJpegOrPngWhateverTheirDeclaredType() throws Exception {
        MockMultipartFile fake = new MockMultipartFile("file", "foto.jpg", "image/jpeg", "no soy una imagen".getBytes());
        mvc.perform(multipart("/evidences").file(fake).with(user(UUID.randomUUID(), "CITIZEN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsPhotosOverTheConfiguredMaximumSize() throws Exception {
        byte[] big = new byte[2048];
        System.arraycopy(JPEG, 0, big, 0, JPEG.length);
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", big);
        mvc.perform(multipart("/evidences").file(file).with(user(UUID.randomUUID(), "CITIZEN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownEvidenceAndUnauthorizedWithoutToken() throws Exception {
        mvc.perform(get("/evidences/" + UUID.randomUUID()).with(user(UUID.randomUUID(), "OPERATOR")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/evidences/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    private String upload(UUID userId, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", content);
        String body = mvc.perform(multipart("/evidences").file(file).with(user(userId, "CITIZEN")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asText();
    }

    private static RequestPostProcessor user(UUID userId, String role) {
        return jwt().jwt(token -> token.subject(userId.toString()).claim("role", role));
    }

    @TestConfiguration
    static class StorageConfig {

        @Bean
        @Primary
        InMemoryStorage inMemoryStorage() {
            return new InMemoryStorage();
        }
    }

    static class InMemoryStorage implements EvidenceStorage {

        final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        @Override
        public void store(String objectKey, byte[] content, String contentType) {
            objects.put(objectKey, content);
        }

        @Override
        public URL presignedDownloadUrl(String objectKey, Duration ttl) {
            try {
                return URI.create("https://signed.test/" + objectKey).toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
