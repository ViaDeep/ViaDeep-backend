package pe.viadeep.auth.domain;

public class DocumentNumberAlreadyRegisteredException extends RuntimeException {

    public DocumentNumberAlreadyRegisteredException(String documentNumber) {
        super("Ya existe una cuenta de operador con el DNI " + documentNumber);
    }
}
