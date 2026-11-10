package pe.viadeep.auth.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Servidor SMTP y remitente de los correos de ViaDeep. Con el puerto 587 se usa STARTTLS, como pide Gmail. Si no hay
 * host, no se envía nada y el correo se escribe en el log.
 */
@ConfigurationProperties(prefix = "viadeep.mail")
record MailProperties(String host, Integer port, String username, String password, String from) {

    MailProperties {
        host = host == null ? "" : host.strip();
        port = port == null ? 587 : port;
        username = username == null ? "" : username.strip();
        password = password == null ? "" : password.replace(" ", "");
        if (from == null || from.isBlank()) {
            from = username.isEmpty() ? "ViaDeep <no-reply@viadeep.local>" : "ViaDeep <" + username + ">";
        }
        from = from.strip();
    }

    boolean configured() {
        return !host.isEmpty();
    }
}
