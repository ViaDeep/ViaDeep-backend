package pe.viadeep.auth.domain;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("El correo " + email + " ya está registrado");
    }
}
