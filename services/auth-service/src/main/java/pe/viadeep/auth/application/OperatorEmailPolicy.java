package pe.viadeep.auth.application;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dominios de correo institucional permitidos para las cuentas de operador (por ejemplo {@code munlima.gob.pe}).
 * Así no hace falta una lista de correos: el dominio prueba que la cuenta pertenece a la municipalidad. Si la lista
 * está vacía se acepta cualquier dominio, lo que solo debe usarse en desarrollo.
 */
@ConfigurationProperties(prefix = "viadeep.operators")
public record OperatorEmailPolicy(List<String> allowedEmailDomains) {

    public OperatorEmailPolicy {
        allowedEmailDomains = allowedEmailDomains == null ? List.of() : allowedEmailDomains.stream()
                .filter(Objects::nonNull)
                .map(domain -> domain.strip().toLowerCase(Locale.ROOT).replaceFirst("^@", ""))
                .filter(domain -> !domain.isEmpty())
                .toList();
    }

    public boolean restricted() {
        return !allowedEmailDomains.isEmpty();
    }

    /** Recibe el correo ya normalizado (en minúsculas). */
    public boolean allows(String normalizedEmail) {
        if (!restricted()) {
            return true;
        }
        String domain = normalizedEmail.substring(normalizedEmail.lastIndexOf('@') + 1);
        return allowedEmailDomains.contains(domain);
    }

    void check(String normalizedEmail) {
        if (!allows(normalizedEmail)) {
            throw new OperatorEmailDomainNotAllowedException(allowedEmailDomains);
        }
    }
}
