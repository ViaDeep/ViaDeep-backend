package pe.viadeep.incident.presentation;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.viadeep.incident.application.AssignIncidentHandler;
import pe.viadeep.incident.application.AssignSeverityHandler;
import pe.viadeep.incident.application.ChangeIncidentStatusHandler;
import pe.viadeep.incident.application.RegisterIncidentCommand;
import pe.viadeep.incident.application.RegisterIncidentHandler;
import pe.viadeep.incident.domain.AiDetection;
import pe.viadeep.incident.domain.GeoLocation;
import pe.viadeep.incident.domain.InvalidIncidentException;
import pe.viadeep.incident.presentation.IncidentDtos.AiResultRequest;
import pe.viadeep.incident.presentation.IncidentDtos.AssignIncidentRequest;
import pe.viadeep.incident.presentation.IncidentDtos.AssignSeverityRequest;
import pe.viadeep.incident.presentation.IncidentDtos.ChangeStatusRequest;
import pe.viadeep.incident.presentation.IncidentDtos.IncidentResponse;
import pe.viadeep.incident.presentation.IncidentDtos.RegisterIncidentRequest;

/** Escrituras (CQRS): cada solicitud se convierte en un comando para su manejador. */
@RestController
@RequestMapping("/incidents")
class IncidentCommandController {

    private final RegisterIncidentHandler registerIncident;
    private final ChangeIncidentStatusHandler changeStatus;
    private final AssignSeverityHandler assignSeverity;
    private final AssignIncidentHandler assignIncident;

    IncidentCommandController(RegisterIncidentHandler registerIncident, ChangeIncidentStatusHandler changeStatus,
                              AssignSeverityHandler assignSeverity, AssignIncidentHandler assignIncident) {
        this.registerIncident = registerIncident;
        this.changeStatus = changeStatus;
        this.assignSeverity = assignSeverity;
        this.assignIncident = assignIncident;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    IncidentResponse register(@Valid @RequestBody RegisterIncidentRequest request, @AuthenticationPrincipal Jwt jwt) {
        RegisterIncidentCommand command = new RegisterIncidentCommand(Jwts.userId(jwt), jwt.getTokenValue(),
                request.evidenceId(), request.type(), new GeoLocation(request.latitude(), request.longitude()),
                request.description(), aiDetection(request));
        return IncidentResponse.from(registerIncident.handle(command));
    }

    @PatchMapping("/{id}/status")
    IncidentResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return IncidentResponse.from(changeStatus.handle(id, request.status(), Jwts.userId(jwt), request.comment()));
    }

    @PatchMapping("/{id}/severity")
    IncidentResponse assignSeverity(@PathVariable UUID id, @Valid @RequestBody AssignSeverityRequest request) {
        return IncidentResponse.from(assignSeverity.handle(id, request.severity()));
    }

    /** Asigna o reasigna el responsable y el área; queda registrado el operador que lo hizo. */
    @PatchMapping("/{id}/assignment")
    IncidentResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignIncidentRequest request,
                            @AuthenticationPrincipal Jwt jwt) {
        return IncidentResponse.from(assignIncident.handle(id, request.assignee(), request.area(), request.note(),
                Jwts.userId(jwt)));
    }

    private static AiDetection aiDetection(RegisterIncidentRequest request) {
        AiResultRequest ai = request.aiResult();
        if (request.aiValidated()) {
            if (ai == null) {
                throw new InvalidIncidentException("Un reporte validado por la IA debe incluir aiResult");
            }
            return new AiDetection(ai.confidence(), ai.bboxX(), ai.bboxY(), ai.bboxWidth(), ai.bboxHeight());
        }
        if (ai != null) {
            throw new InvalidIncidentException("Un registro manual no debe incluir aiResult");
        }
        return null;
    }
}
