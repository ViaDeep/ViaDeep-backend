package pe.viadeep.auth.presentation;

import java.util.NoSuchElementException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.viadeep.auth.application.AccountDisabledException;
import pe.viadeep.auth.application.AdministratorNotAllowedException;
import pe.viadeep.auth.application.CannotDisableOwnAccountException;
import pe.viadeep.auth.application.CannotRemoveOwnAdministratorException;
import pe.viadeep.auth.application.EmailCodeTooSoonException;
import pe.viadeep.auth.application.EmailNotVerifiedException;
import pe.viadeep.auth.application.InvalidEmailCodeException;
import pe.viadeep.auth.application.InvalidCredentialsException;
import pe.viadeep.auth.application.OperatorDocumentLockedException;
import pe.viadeep.auth.application.OperatorEmailDomainNotAllowedException;
import pe.viadeep.auth.application.WrongPasswordException;
import pe.viadeep.auth.domain.DocumentNumberAlreadyRegisteredException;
import pe.viadeep.auth.domain.EmailAlreadyRegisteredException;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail emailTaken(EmailAlreadyRegisteredException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DocumentNumberAlreadyRegisteredException.class)
    ProblemDetail documentTaken(DocumentNumberAlreadyRegisteredException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(OperatorDocumentLockedException.class)
    ProblemDetail documentLocked(OperatorDocumentLockedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    /** 403 y no 401: las credenciales son correctas, pero la cuenta no tiene permiso para entrar. */
    @ExceptionHandler(AccountDisabledException.class)
    ProblemDetail accountDisabled(AccountDisabledException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    /** 403 con un código propio, para que la app abra la pantalla donde se escribe el código del correo. */
    @ExceptionHandler(EmailNotVerifiedException.class)
    ProblemDetail emailNotVerified(EmailNotVerifiedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        problem.setProperty("code", "EMAIL_NOT_VERIFIED");
        return problem;
    }

    @ExceptionHandler(InvalidEmailCodeException.class)
    ProblemDetail invalidEmailCode(InvalidEmailCodeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(EmailCodeTooSoonException.class)
    ResponseEntity<ProblemDetail> emailCodeTooSoon(EmailCodeTooSoonException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        problem.setProperty("retryAfterSeconds", e.retryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSeconds()))
                .body(problem);
    }

    /** El servidor de correo no respondió: el registro o el pedido de código no se guarda y se puede reintentar. */
    @ExceptionHandler(MailException.class)
    ProblemDetail mailUnavailable(MailException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "No pudimos enviar el correo. Intenta de nuevo en unos minutos.");
    }

    @ExceptionHandler(CannotRemoveOwnAdministratorException.class)
    ProblemDetail cannotRemoveOwnAdministrator(CannotRemoveOwnAdministratorException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(AdministratorNotAllowedException.class)
    ProblemDetail administratorNotAllowed(AdministratorNotAllowedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(CannotDisableOwnAccountException.class)
    ProblemDetail cannotDisableOwnAccount(CannotDisableOwnAccountException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** 422 y no 401: la sesión sigue siendo válida, solo falló la confirmación de la contraseña. */
    @ExceptionHandler(WrongPasswordException.class)
    ProblemDetail wrongPassword(WrongPasswordException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(OperatorEmailDomainNotAllowedException.class)
    ProblemDetail operatorEmailDomain(OperatorEmailDomainNotAllowedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail notFound(NoSuchElementException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
