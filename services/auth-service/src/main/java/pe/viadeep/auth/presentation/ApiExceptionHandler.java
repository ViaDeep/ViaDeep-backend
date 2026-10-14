package pe.viadeep.auth.presentation;

import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.viadeep.auth.application.AccountDisabledException;
import pe.viadeep.auth.application.CannotDisableOwnAccountException;
import pe.viadeep.auth.application.InvalidCredentialsException;
import pe.viadeep.auth.application.WrongPasswordException;
import pe.viadeep.auth.domain.EmailAlreadyRegisteredException;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail emailTaken(EmailAlreadyRegisteredException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
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

    @ExceptionHandler(CannotDisableOwnAccountException.class)
    ProblemDetail cannotDisableOwnAccount(CannotDisableOwnAccountException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** 422 y no 401: la sesión sigue siendo válida, solo falló la confirmación de la contraseña. */
    @ExceptionHandler(WrongPasswordException.class)
    ProblemDetail wrongPassword(WrongPasswordException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail notFound(NoSuchElementException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
