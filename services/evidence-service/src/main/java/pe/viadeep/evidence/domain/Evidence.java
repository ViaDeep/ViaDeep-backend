package pe.viadeep.evidence.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Metadatos de una fotografía de evidencia. El archivo vive en el almacenamiento de objetos;
 * aquí solo se guarda su clave. No referencia a la incidencia: es la incidencia la que guarda evidence_id.
 */
public final class Evidence {

    private final UUID id;
    private final String objectKey;
    private final ImageFormat format;
    private final long sizeBytes;
    private final UUID uploadedBy;
    private final Instant uploadedAt;

    public Evidence(UUID id, String objectKey, ImageFormat format, long sizeBytes, UUID uploadedBy, Instant uploadedAt) {
        this.id = Objects.requireNonNull(id);
        this.objectKey = Objects.requireNonNull(objectKey);
        this.format = Objects.requireNonNull(format);
        this.sizeBytes = sizeBytes;
        this.uploadedBy = Objects.requireNonNull(uploadedBy);
        this.uploadedAt = Objects.requireNonNull(uploadedAt);
    }

    public static Evidence upload(ImageFormat format, long sizeBytes, long maxSizeBytes, UUID uploadedBy, Instant now) {
        if (sizeBytes <= 0) {
            throw new InvalidEvidenceException("La fotografía está vacía");
        }
        if (sizeBytes > maxSizeBytes) {
            throw new InvalidEvidenceException("La fotografía supera el tamaño máximo de " + maxSizeBytes + " bytes");
        }
        UUID id = UUID.randomUUID();
        String objectKey = "evidences/%s/%s.%s".formatted(uploadedBy, id, format.extension());
        return new Evidence(id, objectKey, format, sizeBytes, uploadedBy, now);
    }

    /** Solo el autor de la evidencia o un operador municipal pueden verla. */
    public boolean canBeViewedBy(UUID userId, boolean isOperator) {
        return isOperator || uploadedBy.equals(userId);
    }

    public UUID id() {
        return id;
    }

    public String objectKey() {
        return objectKey;
    }

    public ImageFormat format() {
        return format;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public UUID uploadedBy() {
        return uploadedBy;
    }

    public Instant uploadedAt() {
        return uploadedAt;
    }
}
