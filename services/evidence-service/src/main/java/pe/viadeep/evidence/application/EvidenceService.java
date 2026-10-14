package pe.viadeep.evidence.application;

import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.evidence.domain.Evidence;
import pe.viadeep.evidence.domain.EvidenceRepository;
import pe.viadeep.evidence.domain.ImageFormat;
import pe.viadeep.evidence.domain.InvalidEvidenceException;

@Service
public class EvidenceService {

    private final EvidenceRepository evidences;
    private final EvidenceStorage storage;
    private final EvidenceProperties properties;
    private final Clock clock;

    public EvidenceService(EvidenceRepository evidences, EvidenceStorage storage, EvidenceProperties properties, Clock clock) {
        this.evidences = evidences;
        this.storage = storage;
        this.properties = properties;
        this.clock = clock;
    }

    /** Valida la fotografía, la sube al almacenamiento y registra sus metadatos. */
    @Transactional
    public Evidence upload(byte[] content, UUID uploadedBy) {
        ImageFormat format = ImageFormat.detect(content)
                .orElseThrow(() -> new InvalidEvidenceException("Solo se aceptan fotografías JPEG o PNG"));
        Evidence evidence = Evidence.upload(format, content.length, properties.maxSizeBytes(), uploadedBy, clock.instant());
        storage.store(evidence.objectKey(), content, format.contentType());
        return evidences.save(evidence);
    }

    @Transactional(readOnly = true)
    public Evidence get(UUID id, Requester requester) {
        Evidence evidence = evidences.findById(id).orElseThrow(() -> new EvidenceNotFoundException(id));
        if (!evidence.canBeViewedBy(requester.userId(), requester.isOperator())) {
            throw new EvidenceAccessDeniedException();
        }
        return evidence;
    }

    public SignedUrl signedUrl(UUID id, Requester requester) {
        Evidence evidence = get(id, requester);
        URL url = storage.presignedDownloadUrl(evidence.objectKey(), properties.urlTtl());
        return new SignedUrl(url, clock.instant().plus(properties.urlTtl()));
    }

    public record SignedUrl(URL url, Instant expiresAt) {
    }
}
