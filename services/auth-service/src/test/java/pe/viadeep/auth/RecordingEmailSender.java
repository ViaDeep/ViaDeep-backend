package pe.viadeep.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import pe.viadeep.auth.application.EmailSender;

/** En las pruebas los correos no se envían: se guardan para leer el código que habría recibido el usuario. */
@Component
@Primary
class RecordingEmailSender implements EmailSender {

    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    record SentEmail(String to, String subject, String body) {
    }

    private final List<SentEmail> sent = new ArrayList<>();

    @Override
    public synchronized void send(String to, String subject, String body) {
        sent.add(new SentEmail(to, subject, body));
    }

    synchronized List<SentEmail> sentTo(String to) {
        return sent.stream().filter(email -> email.to().equals(to)).toList();
    }

    /** El código del último correo enviado a esa dirección. */
    synchronized String lastCode(String to) {
        List<SentEmail> emails = sentTo(to);
        Matcher matcher = CODE.matcher(emails.get(emails.size() - 1).body());
        if (!matcher.find()) {
            throw new AssertionError("El correo no tiene un código de 6 dígitos");
        }
        return matcher.group(1);
    }
}
