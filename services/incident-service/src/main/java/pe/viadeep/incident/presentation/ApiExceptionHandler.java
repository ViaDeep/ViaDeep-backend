package pe.viadeep.incident.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.viadeep.incident.application.EvidenceRejectedException;
import pe.viadeep.incident.application.EvidenceServiceUnavailableException;
import pe.viadeep.incident.domain.IncidentClosedException;
import pe.viadeep.incident.domain.IncidentNotFoundException;
import pe.viadeep.incident.domain.InvalidIncidentException;
import pe.viadeep.incident.domain.InvalidStatusTransitionException;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(InvalidIncidentException.class)
    ProblemDetail invalid(InvalidIncidentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(EvidenceRejectedException.class)
    ProblemDetail evidenceRejected(EvidenceRejectedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    ProblemDetail invalidTransition(InvalidStatusTransitionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IncidentClosedException.class)
    ProblemDetail incidentClosed(IncidentClosedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IncidentNotFoundException.class)
    ProblemDetail notFound(IncidentNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(EvidenceServiceUnavailableException.class)
    ProblemDetail evidenceUnavailable(EvidenceServiceUnavailableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
}
