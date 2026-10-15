package pe.viadeep.incident.domain;

/** La descripción del ciudadano no cumple las reglas de {@link DescriptionPolicy}. */
public class InappropriateDescriptionException extends RuntimeException {

    public InappropriateDescriptionException(String message) {
        super(message);
    }
}
