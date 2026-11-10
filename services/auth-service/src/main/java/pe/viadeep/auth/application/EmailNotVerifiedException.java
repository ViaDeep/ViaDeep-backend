package pe.viadeep.auth.application;

/** La contraseña es correcta, pero el ciudadano aún no confirmó su correo con el código. */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Confirma tu correo con el código que te enviamos antes de iniciar sesión");
    }
}
