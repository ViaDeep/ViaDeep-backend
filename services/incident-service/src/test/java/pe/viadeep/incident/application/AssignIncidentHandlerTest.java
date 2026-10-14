package pe.viadeep.incident.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.viadeep.incident.domain.GeoLocation;
import pe.viadeep.incident.domain.Incident;
import pe.viadeep.incident.domain.IncidentClosedException;
import pe.viadeep.incident.domain.IncidentNotFoundException;
import pe.viadeep.incident.domain.IncidentRepository;
import pe.viadeep.incident.domain.IncidentStatus;
import pe.viadeep.incident.domain.IncidentType;

class AssignIncidentHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-14T15:00:00Z");
    private static final UUID OPERATOR = UUID.randomUUID();

    private final IncidentRepository incidents = mock(IncidentRepository.class);
    private final AssignIncidentHandler handler = new AssignIncidentHandler(incidents,
            Clock.fixed(NOW.plusSeconds(300), ZoneOffset.UTC));

    @Test
    void assignsWithTheOperatorAndTheCurrentTime() {
        Incident incident = stored();
        when(incidents.save(any())).thenAnswer(call -> call.getArgument(0));

        Incident saved = handler.handle(incident.id(), "Juan Pérez", "Obras Públicas", "Prioridad alta", OPERATOR);

        assertThat(saved.assignment()).hasValueSatisfying(assignment -> {
            assertThat(assignment.assignee()).isEqualTo("Juan Pérez");
            assertThat(assignment.area()).isEqualTo("Obras Públicas");
            assertThat(assignment.note()).isEqualTo("Prioridad alta");
            assertThat(assignment.assignedBy()).isEqualTo(OPERATOR);
            assertThat(assignment.assignedAt()).isEqualTo(NOW.plusSeconds(300));
        });
        assertThat(saved.updatedAt()).isEqualTo(NOW.plusSeconds(300));
        verify(incidents).save(incident);
    }

    @Test
    void failsWhenTheIncidentDoesNotExist() {
        UUID missing = UUID.randomUUID();
        when(incidents.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(missing, "Juan", "Obras", null, OPERATOR))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    @Test
    void doesNotSaveAClosedIncident() {
        Incident incident = stored();
        incident.changeStatus(IncidentStatus.REJECTED, OPERATOR, "No es una pista", NOW);

        assertThatThrownBy(() -> handler.handle(incident.id(), "Juan", "Obras", null, OPERATOR))
                .isInstanceOf(IncidentClosedException.class);
        verify(incidents, never()).save(any());
    }

    private Incident stored() {
        Incident incident = Incident.register(UUID.randomUUID(), UUID.randomUUID(), IncidentType.D40,
                new GeoLocation(-12.0464, -77.0428), null, null, null, NOW);
        when(incidents.findById(incident.id())).thenReturn(Optional.of(incident));
        return incident;
    }
}
