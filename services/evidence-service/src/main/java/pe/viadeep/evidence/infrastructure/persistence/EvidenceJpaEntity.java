package pe.viadeep.evidence.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import pe.viadeep.evidence.domain.Evidence;
import pe.viadeep.evidence.domain.ImageFormat;

@Entity
@Table(name = "evidences")
class EvidenceJpaEntity {

    @Id
    private UUID id;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected EvidenceJpaEntity() {
    }

    static EvidenceJpaEntity fromDomain(Evidence evidence) {
        EvidenceJpaEntity entity = new EvidenceJpaEntity();
        entity.id = evidence.id();
        entity.objectKey = evidence.objectKey();
        entity.contentType = evidence.format().contentType();
        entity.sizeBytes = evidence.sizeBytes();
        entity.uploadedBy = evidence.uploadedBy();
        entity.uploadedAt = evidence.uploadedAt();
        return entity;
    }

    Evidence toDomain() {
        return new Evidence(id, objectKey, ImageFormat.fromContentType(contentType), sizeBytes, uploadedBy, uploadedAt);
    }
}
