package pe.viadeep.incident.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import pe.viadeep.incident.domain.AiDetection;
import pe.viadeep.incident.domain.DetectedDamage;

/** Fila de incident_detections: una detección de LMD-YOLO en la foto del reporte. */
@Embeddable
class DetectionEmbeddable {

    @Column(name = "incident_type_id", nullable = false)
    private short incidentTypeId;

    @Column(nullable = false)
    private float confidence;

    @Column(name = "bbox_x", nullable = false)
    private float bboxX;

    @Column(name = "bbox_y", nullable = false)
    private float bboxY;

    @Column(name = "bbox_width", nullable = false)
    private float bboxWidth;

    @Column(name = "bbox_height", nullable = false)
    private float bboxHeight;

    @Column(nullable = false)
    private boolean accepted;

    protected DetectionEmbeddable() {
    }

    static DetectionEmbeddable fromDomain(DetectedDamage damage) {
        DetectionEmbeddable row = new DetectionEmbeddable();
        AiDetection detection = damage.detection();
        row.incidentTypeId = IncidentTypeIds.idOf(damage.type());
        row.confidence = (float) detection.confidence();
        row.bboxX = (float) detection.bboxX();
        row.bboxY = (float) detection.bboxY();
        row.bboxWidth = (float) detection.bboxWidth();
        row.bboxHeight = (float) detection.bboxHeight();
        row.accepted = damage.accepted();
        return row;
    }

    DetectedDamage toDomain() {
        return new DetectedDamage(IncidentTypeIds.typeOf(incidentTypeId),
                new AiDetection(confidence, bboxX, bboxY, bboxWidth, bboxHeight), accepted);
    }
}
