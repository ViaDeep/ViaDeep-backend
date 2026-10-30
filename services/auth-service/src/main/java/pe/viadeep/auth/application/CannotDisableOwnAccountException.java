package pe.viadeep.auth.application;

/** Un operador intentó desactivar su propia cuenta. */
public class CannotDisableOwnAccountException extends RuntimeException {

    public CannotDisableOwnAccountException() {
        super("No puedes desactivar tu propia cuenta");
    }
}
