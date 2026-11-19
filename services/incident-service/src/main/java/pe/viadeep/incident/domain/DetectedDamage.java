package pe.viadeep.incident.domain;

import java.util.Objects;

/**
 * Un daño que LMD-YOLO reconoció en la foto. accepted es true si superó el umbral de confianza de la app; los que no
 * lo superaron se conservan como referencia para el operador.
 */
public record DetectedDamage(IncidentType type, AiDetection detection, boolean accepted) {

    public DetectedDamage {
        Objects.requireNonNull(type);
        Objects.requireNonNull(detection);
    }
}
