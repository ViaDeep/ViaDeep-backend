package pe.viadeep.auth.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import pe.viadeep.auth.application.EmailSender;

@Component
@EnableConfigurationProperties(MailProperties.class)
class SmtpEmailSender implements EmailSender {

    private static final Logger log =
            LoggerFactory.getLogger(SmtpEmailSender.class);

    private final MailProperties properties;
    private final JavaMailSender sender;

    SmtpEmailSender(
            MailProperties properties,
            JavaMailSender sender
    ) {
        this.properties = properties;
        this.sender = sender;
    }

    /**
     * Spring Boot solo crea el JavaMailSender si hay spring.mail.host (perfil aws). Sin él, en desarrollo y en las
     * pruebas de docker compose, el correo solo se escribe en el log, como antes.
     */
    @org.springframework.beans.factory.annotation.Autowired
    SmtpEmailSender(
            MailProperties properties,
            ObjectProvider<JavaMailSender> sender
    ) {
        this(properties, sender.getIfAvailable());
    }

    @Override
    public void send(String to, String subject, String body) {
        if (sender == null) {
            log.warn("Sin servidor de correo (MAIL_HOST vacío). Correo para {}: {}\n{}", to, subject, body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(properties.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        deliver(sender, message);
    }

    static void deliver(
            JavaMailSender sender,
            SimpleMailMessage message
    ) {
        try {
            sender.send(message);
        } catch (MailAuthenticationException exception) {
            log.error("El servidor SMTP rechazó la autenticación (causa: {})", causes(exception));
            throw exception;
        } catch (MailException exception) {
            log.error("No se pudo enviar el correo por SMTP (causa: {})", causes(exception));
            throw exception;
        }
    }

    /**
     * Solo los nombres de las clases de la cadena de causas (por ejemplo AuthenticationFailedException,
     * UnknownHostException o SocketTimeoutException): bastan para saber si falló la clave, el servidor o la red, sin
     * escribir en el log mensajes que pueden traer el usuario, el destinatario o el código.
     */
    static String causes(Throwable exception) {
        StringBuilder names = new StringBuilder(chain(exception));
        // Si el servidor rechazó el mensaje ya conectado (remitente o destinatario), Spring guarda el error de cada
        // mensaje aparte y no como causa.
        if (exception instanceof MailSendException send) {
            send.getFailedMessages().values().forEach(failure -> names.append(" | ").append(chain(failure)));
        }
        return names.toString();
    }

    private static String chain(Throwable exception) {
        StringBuilder names = new StringBuilder();
        Throwable current = exception;
        for (int depth = 0; current != null && depth < 6; depth++) {
            if (names.length() > 0) {
                names.append(" <- ");
            }
            names.append(current.getClass().getSimpleName());
            current = current.getCause() == current ? null : current.getCause();
        }
        return names.toString();
    }
}
