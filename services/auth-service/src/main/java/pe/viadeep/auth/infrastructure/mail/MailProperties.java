package pe.viadeep.auth.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "viadeep.mail")
record MailProperties(String from) {

    MailProperties {
        from = from == null ? "" : from.strip();
    }
}