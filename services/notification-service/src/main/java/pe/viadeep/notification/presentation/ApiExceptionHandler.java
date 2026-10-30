package pe.viadeep.notification.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.viadeep.notification.application.NotificationNotFoundException;
import pe.viadeep.notification.domain.InvalidNotificationException;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(InvalidNotificationException.class)
    ProblemDetail invalid(InvalidNotificationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    ProblemDetail notFound(NotificationNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
