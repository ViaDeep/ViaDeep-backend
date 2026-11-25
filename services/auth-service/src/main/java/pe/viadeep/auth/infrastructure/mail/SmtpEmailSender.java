package pe.viadeep.auth.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
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

    @Override
    public void send(String to, String subject, String body) {
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
            log.error("El servidor SMTP rechazó la autenticación");
            throw exception;
        } catch (MailException exception) {
            log.error("No se pudo enviar el correo por SMTP");
            throw exception;
        }
    }
}