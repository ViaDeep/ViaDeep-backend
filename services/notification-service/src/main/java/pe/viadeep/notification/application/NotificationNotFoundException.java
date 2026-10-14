package pe.viadeep.notification.application;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID id) {
        super("No existe el aviso " + id);
    }
}
