package pe.viadeep.auth.application;

import java.util.List;
import java.util.stream.Collectors;

public class OperatorEmailDomainNotAllowedException extends RuntimeException {

    public OperatorEmailDomainNotAllowedException(List<String> allowedDomains) {
        super("Las cuentas de operador deben usar un correo institucional ("
                + allowedDomains.stream().map(domain -> "@" + domain).collect(Collectors.joining(", ")) + ")");
    }
}
