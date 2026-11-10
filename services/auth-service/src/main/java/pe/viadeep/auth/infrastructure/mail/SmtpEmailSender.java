package pe.viadeep.auth.infrastructure.mail;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import pe.viadeep.auth.application.EmailSender;

/**
 * Envía los correos por SMTP (Gmail con una contraseña de aplicación). Si no se configuró MAIL_HOST, solo escribe el
 * correo en el log, para poder desarrollar sin enviar nada.
 */
@Component
@EnableConfigurationProperties(MailProperties.class)
class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final MailProperties properties;
    private final JavaMailSenderImpl sender;

    SmtpEmailSender(MailProperties properties) {
        this.properties = properties;
        this.sender = properties.configured() ? smtp(properties) : null;
    }

    private static JavaMailSenderImpl smtp(MailProperties properties) {
        JavaMailSenderImpl smtp = new JavaMailSenderImpl();
        smtp.setHost(properties.host());
        smtp.setPort(properties.port());
        smtp.setDefaultEncoding("UTF-8");
        Properties javaMail = smtp.getJavaMailProperties();
        if (!properties.username().isEmpty()) {
            smtp.setUsername(properties.username());
            smtp.setPassword(properties.password());
            javaMail.put("mail.smtp.auth", "true");
        }
        javaMail.put("mail.smtp.starttls.enable", String.valueOf(properties.port() == 587));
        javaMail.put("mail.smtp.connectiontimeout", "10000");
        javaMail.put("mail.smtp.timeout", "10000");
        javaMail.put("mail.smtp.writetimeout", "10000");
        return smtp;
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
        sender.send(message);
    }
}
