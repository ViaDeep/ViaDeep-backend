package pe.viadeep.incident.infrastructure.evidence;

import java.net.http.HttpClient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.viadeep.incident.application.EvidenceGateway;
import pe.viadeep.incident.application.EvidenceServiceUnavailableException;

/** Invoca GET /evidences/{id} reenviando el JWT del usuario, con un tiempo de espera acotado. */
@Component
@EnableConfigurationProperties(EvidenceServiceProperties.class)
class EvidenceClient implements EvidenceGateway {

    private final RestClient rest;

    EvidenceClient(RestClient.Builder builder, EvidenceServiceProperties properties) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(properties.timeout());
        this.rest = builder.baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }

    @Override
    public Optional<VerifiedEvidence> find(UUID evidenceId, String accessToken) {
        try {
            EvidenceTranslator.EvidenceResponse response = rest.get()
                    .uri("/evidences/{id}", evidenceId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(EvidenceTranslator.EvidenceResponse.class);
            return Optional.ofNullable(response).map(EvidenceTranslator::toVerifiedEvidence);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND) || e.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN)) {
                return Optional.empty();
            }
            throw new EvidenceServiceUnavailableException(e);
        } catch (RestClientException e) {
            throw new EvidenceServiceUnavailableException(e);
        }
    }
}
