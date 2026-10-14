package pe.viadeep.evidence.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import pe.viadeep.evidence.application.EvidenceAccessDeniedException;
import pe.viadeep.evidence.application.EvidenceNotFoundException;
import pe.viadeep.evidence.domain.InvalidEvidenceException;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(InvalidEvidenceException.class)
    ProblemDetail invalid(InvalidEvidenceException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ProblemDetail missingFile(MissingServletRequestPartException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Falta la fotografía en el campo 'file'");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tooLarge(MaxUploadSizeExceededException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "La fotografía supera el tamaño máximo permitido");
    }

    @ExceptionHandler(EvidenceNotFoundException.class)
    ProblemDetail notFound(EvidenceNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(EvidenceAccessDeniedException.class)
    ProblemDetail forbidden(EvidenceAccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }
}
