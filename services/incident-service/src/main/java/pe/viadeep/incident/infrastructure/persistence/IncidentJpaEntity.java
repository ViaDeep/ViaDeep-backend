package pe.viadeep.incident.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import pe.viadeep.incident.domain.AiDetection;
import pe.viadeep.incident.domain.Assignment;
import pe.viadeep.incident.domain.GeoLocation;
import pe.viadeep.incident.domain.Incident;
import pe.viadeep.incident.domain.IncidentStatus;
import pe.viadeep.incident.domain.Severity;

@Entity
@Table(name = "incidents")
class IncidentJpaEntity {

    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    private UUID id;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "evidence_id", nullable = false, unique = true)
    private UUID evidenceId;

    @Column(name = "incident_type_id", nullable = false)
    private short incidentTypeId;

    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(length = 80)
    private String district;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Severity severity;

    @Column(name = "ai_validated", nullable = false)
    private boolean aiValidated;

    private Float confidence;

    @Column(name = "bbox_x")
    private Float bboxX;

    @Column(name = "bbox_y")
    private Float bboxY;

    @Column(name = "bbox_width")
    private Float bboxWidth;

    @Column(name = "bbox_height")
    private Float bboxHeight;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(length = 120)
    private String assignee;

    @Column(name = "assigned_area", length = 120)
    private String assignedArea;

    @Column(name = "assignment_note", length = 500)
    private String assignmentNote;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "incident_id", nullable = false, updatable = false)
    @OrderBy("changedAt ASC")
    private List<StatusHistoryJpaEntity> history = new ArrayList<>();

    protected IncidentJpaEntity() {
    }

    static IncidentJpaEntity fromDomain(Incident incident) {
        IncidentJpaEntity entity = new IncidentJpaEntity();
        entity.id = incident.id();
        entity.reporterId = incident.reporterId();
        entity.evidenceId = incident.evidenceId();
        entity.incidentTypeId = IncidentTypeIds.idOf(incident.type());
        entity.location = WGS84.createPoint(new Coordinate(incident.location().longitude(), incident.location().latitude()));
        entity.district = incident.district().orElse(null);
        entity.description = incident.description().orElse(null);
        entity.status = incident.status();
        entity.severity = incident.severity().orElse(null);
        entity.aiValidated = incident.aiValidated();
        incident.aiDetection().ifPresent(ai -> {
            entity.confidence = (float) ai.confidence();
            entity.bboxX = (float) ai.bboxX();
            entity.bboxY = (float) ai.bboxY();
            entity.bboxWidth = (float) ai.bboxWidth();
            entity.bboxHeight = (float) ai.bboxHeight();
        });
        entity.createdAt = incident.createdAt();
        entity.updatedAt = incident.updatedAt();
        incident.assignment().ifPresent(assignment -> {
            entity.assignee = assignment.assignee();
            entity.assignedArea = assignment.area();
            entity.assignmentNote = assignment.note();
            entity.assignedBy = assignment.assignedBy();
            entity.assignedAt = assignment.assignedAt();
        });
        incident.history().forEach(change -> entity.history.add(StatusHistoryJpaEntity.fromDomain(change)));
        return entity;
    }

    Incident toDomain() {
        AiDetection ai = aiValidated ? new AiDetection(confidence, bboxX, bboxY, bboxWidth, bboxHeight) : null;
        Assignment assignment = assignee == null ? null
                : new Assignment(assignee, assignedArea, assignmentNote, assignedBy, assignedAt);
        return new Incident(id, reporterId, evidenceId, IncidentTypeIds.typeOf(incidentTypeId),
                new GeoLocation(location.getY(), location.getX()), district, description, status, severity, ai,
                createdAt, updatedAt, history.stream().map(StatusHistoryJpaEntity::toDomain).toList(),
                assignment);
    }
}
