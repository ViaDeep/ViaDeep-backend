package pe.viadeep.auth.application;

/** El servidor no tiene configurado el identificador de la app en Google o Microsoft. */
public class SocialLoginUnavailableException extends RuntimeException {

    public SocialLoginUnavailableException(IdentityProvider provider) {
        super("El inicio de sesión con " + (provider == IdentityProvider.GOOGLE ? "Google" : "Microsoft")
                + " no está disponible por ahora. Usa tu correo y contraseña.");
    }
}
