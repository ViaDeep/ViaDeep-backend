package pe.viadeep.auth.application;

/** La contraseña actual no coincide al intentar cambiarla. */
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException() {
        super("La contraseña actual no es correcta");
    }
}
