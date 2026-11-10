package pe.viadeep.auth.application;

/** El código no coincide, ya venció o se agotaron los intentos. */
public class InvalidEmailCodeException extends RuntimeException {

    public InvalidEmailCodeException(String message) {
        super(message);
    }
}
