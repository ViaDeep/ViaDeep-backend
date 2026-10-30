package pe.viadeep.evidence.presentation;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.viadeep.evidence.application.EvidenceService;
import pe.viadeep.evidence.application.Requester;

/** Interfaz pública del contexto Evidencias, para los clientes y para el EvidenceClient de Incidencias. */
@RestController
@RequestMapping("/evidences")
class EvidenceController {

    private final EvidenceService evidenceService;

    EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    EvidenceResponse upload(@RequestPart("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) throws IOException {
        return EvidenceResponse.from(evidenceService.upload(file.getBytes(), userId(jwt)));
    }

    @GetMapping("/{id}")
    EvidenceResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return EvidenceResponse.from(evidenceService.get(id, requester(jwt)));
    }

    @GetMapping("/{id}/url")
    SignedUrlResponse signedUrl(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        EvidenceService.SignedUrl signed = evidenceService.signedUrl(id, requester(jwt));
        return new SignedUrlResponse(signed.url().toString(), signed.expiresAt());
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static Requester requester(Jwt jwt) {
        return new Requester(userId(jwt), "OPERATOR".equals(jwt.getClaimAsString("role")));
    }

    record SignedUrlResponse(String url, Instant expiresAt) {
    }
}
