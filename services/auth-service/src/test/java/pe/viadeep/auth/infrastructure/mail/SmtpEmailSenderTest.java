package pe.viadeep.auth.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(OutputCaptureExtension.class)
class SmtpEmailSenderTest {

    @Test
    void sendsEmailUsingSpringManagedJavaMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);

        SmtpEmailSender sender = new SmtpEmailSender(
                new MailProperties("viadeep.tesis@gmail.com"),
                mailSender
        );

        sender.send(
                "persona@example.com",
                "Codigo de verificacion",
                "Tu codigo es 123456"
        );

        org.mockito.ArgumentCaptor<SimpleMailMessage> captor =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("viadeep.tesis@gmail.com");
        assertThat(message.getTo()).containsExactly("persona@example.com");
        assertThat(message.getSubject()).isEqualTo("Codigo de verificacion");
        assertThat(message.getText()).isEqualTo("Tu codigo es 123456");
    }

    @Test
    void anSmtpFailureDoesNotExposeSensitiveInformationInLogs(
            CapturedOutput output
    ) {
        JavaMailSender mailSender = mock(JavaMailSender.class);

        doThrow(new MailSendException(
                "smtp-user/smtp-password rejected for persona@example.com"
        )).when(mailSender).send(any(SimpleMailMessage.class));

        SmtpEmailSender sender = new SmtpEmailSender(
                new MailProperties("viadeep.tesis@gmail.com"),
                mailSender
        );

        assertThatThrownBy(() -> sender.send(
                "persona@example.com",
                "Codigo temporal 123456",
                "El codigo secreto es 123456"
        )).isInstanceOf(MailSendException.class);

        assertThat(output.getAll())
                .contains("No se pudo enviar el correo por SMTP")
                .doesNotContain(
                        "smtp-user",
                        "smtp-password",
                        "persona@example.com",
                        "123456"
                );
    }
}