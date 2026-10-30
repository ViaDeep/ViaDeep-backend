package pe.viadeep.auth.application;

public class OperatorDocumentLockedException extends RuntimeException {

    public OperatorDocumentLockedException() {
        super("El DNI de una cuenta de operador no se puede cambiar. Pídeselo al administrador.");
    }
}
