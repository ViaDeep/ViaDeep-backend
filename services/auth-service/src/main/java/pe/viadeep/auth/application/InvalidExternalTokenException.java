package pe.viadeep.auth.application;

/** El token de Google o Microsoft no es válido, venció o no trae un correo verificado. */
public class InvalidExternalTokenException extends RuntimeException {

    public InvalidExternalTokenException(String message) {
        super(message);
    }
}
