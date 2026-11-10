package pe.viadeep.auth.application;

/** Las cuentas de operador municipal solo entran con su correo institucional y contraseña. */
public class SocialLoginNotAllowedException extends RuntimeException {

    public SocialLoginNotAllowedException() {
        super("Esta cuenta es de operador municipal: inicia sesión con tu correo y contraseña");
    }
}
