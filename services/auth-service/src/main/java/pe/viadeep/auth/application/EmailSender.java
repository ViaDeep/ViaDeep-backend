package pe.viadeep.auth.application;

/** Envía un correo de texto al usuario. */
public interface EmailSender {

    void send(String to, String subject, String body);
}
