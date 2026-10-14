package pe.viadeep.auth.application;

public class AdministratorNotAllowedException extends RuntimeException {

    public AdministratorNotAllowedException() {
        super("Solo una cuenta de operador activa puede ser administrador");
    }
}
