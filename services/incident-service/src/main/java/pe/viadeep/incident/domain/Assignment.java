package pe.viadeep.incident.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Responsable (persona y área) a cargo de atender una incidencia, con una nota opcional para la cuadrilla.
 * assignedBy es el operador que hizo la asignación.
 */
public record Assignment(String assignee, String area, String note, UUID assignedBy, Instant assignedAt) {

    public static final int MAX_NAME_LENGTH = 120;
    public static final int MAX_NOTE_LENGTH = 500;

    public Assignment {
        assignee = required("El responsable", assignee);
        area = required("El área", area);
        note = note == null || note.isBlank() ? null : note.strip();
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new InvalidIncidentException("La nota de asignación no puede superar " + MAX_NOTE_LENGTH + " caracteres");
        }
        Objects.requireNonNull(assignedBy);
        Objects.requireNonNull(assignedAt);
    }

    private static String required(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidIncidentException(label + " es obligatorio");
        }
        String clean = value.strip();
        if (clean.length() > MAX_NAME_LENGTH) {
            throw new InvalidIncidentException(label + " no puede superar " + MAX_NAME_LENGTH + " caracteres");
        }
        return clean;
    }
}
