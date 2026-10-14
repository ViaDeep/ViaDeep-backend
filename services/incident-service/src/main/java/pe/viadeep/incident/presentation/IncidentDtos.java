package pe.viadeep.incident.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import pe.viadeep.incident.domain.Incident;
import pe.viadeep.incident.domain.IncidentStatus;
import pe.viadeep.incident.domain.IncidentType;
import pe.viadeep.incident.domain.Severity;

final class IncidentDtos {

    private IncidentDtos() {
    }

    /**
     * Reporte enviado por la app. Si aiValidated es true, aiResult es obligatorio (clase, confianza y bounding box
     * de LMD-YOLO); si es false, es un registro manual y el ciudadano elige el tipo.
     */
    record RegisterIncidentRequest(
            @NotNull UUID evidenceId,
            @NotNull IncidentType type,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @Size(max = 500) String description,
            @NotNull Boolean aiValidated,
            @Valid AiResultRequest aiResult) {
    }

    record AiResultRequest(
            @NotNull @DecimalMin("0") @DecimalMax("1") Double confidence,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double bboxX,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double bboxY,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double bboxWidth,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double bboxHeight) {
    }

    record ChangeStatusRequest(@NotNull IncidentStatus status, @Size(max = 500) String comment) {
    }

    record AssignSeverityRequest(@NotNull Severity severity) {
    }

    record IncidentResponse(UUID id, IncidentType type, IncidentStatus status, Severity severity, boolean aiValidated,
                            Instant createdAt, Instant updatedAt) {

        static IncidentResponse from(Incident incident) {
            return new IncidentResponse(incident.id(), incident.type(), incident.status(),
                    incident.severity().orElse(null), incident.aiValidated(), incident.createdAt(),
                    incident.updatedAt());
        }
    }
}
