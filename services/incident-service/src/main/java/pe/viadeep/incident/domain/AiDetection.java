package pe.viadeep.incident.domain;

/**
 * Resultado de la inferencia on-device: confianza y bounding box normalizados entre 0 y 1.
 * Un reporte manual no tiene AiDetection.
 */
public record AiDetection(double confidence, double bboxX, double bboxY, double bboxWidth, double bboxHeight) {

    public AiDetection {
        requireUnit("confidence", confidence);
        requireUnit("bboxX", bboxX);
        requireUnit("bboxY", bboxY);
        requireUnit("bboxWidth", bboxWidth);
        requireUnit("bboxHeight", bboxHeight);
    }

    private static void requireUnit(String name, double value) {
        if (Double.isNaN(value) || value < 0 || value > 1) {
            throw new InvalidIncidentException(name + " debe estar entre 0 y 1");
        }
    }
}
