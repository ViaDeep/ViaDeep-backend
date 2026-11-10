package pe.viadeep.auth.application;

/** Se pidió otro código al mismo correo antes de que pase el tiempo mínimo entre envíos. */
public class EmailCodeTooSoonException extends RuntimeException {

    private final long retryAfterSeconds;

    public EmailCodeTooSoonException(long retryAfterSeconds) {
        super("Espera " + retryAfterSeconds + " segundos antes de pedir otro código");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
