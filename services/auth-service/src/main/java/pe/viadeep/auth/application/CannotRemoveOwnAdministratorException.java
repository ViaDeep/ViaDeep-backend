package pe.viadeep.auth.application;

/** Un administrador intentó quitarse su propio permiso; así la plataforma nunca se queda sin administrador. */
public class CannotRemoveOwnAdministratorException extends RuntimeException {

    public CannotRemoveOwnAdministratorException() {
        super("No puedes quitarte tu propio permiso de administrador");
    }
}
