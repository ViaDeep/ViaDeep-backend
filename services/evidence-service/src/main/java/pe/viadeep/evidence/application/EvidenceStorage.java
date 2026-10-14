package pe.viadeep.evidence.application;

import java.net.URL;
import java.time.Duration;

/** Contrato para guardar el archivo de una evidencia y generar su URL firmada. */
public interface EvidenceStorage {

    void store(String objectKey, byte[] content, String contentType);

    URL presignedDownloadUrl(String objectKey, Duration ttl);
}
