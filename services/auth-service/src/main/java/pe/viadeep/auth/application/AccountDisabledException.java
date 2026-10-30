package pe.viadeep.auth.application;

/** Las credenciales son correctas, pero un operador desactivó la cuenta. */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("Tu cuenta está desactivada. Contacta a la municipalidad.");
    }
}
