package pe.viadeep.incident.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentTest {

    private static final Instant NOW = Instant.parse("2026-10-14T15:00:00Z");
    private static final UUID CITIZEN = UUID.randomUUID();
    private static final UUID OPERATOR = UUID.randomUUID();

    @Test
    void registersInPendingWithInitialHistoryByTheReporter() {
        Incident incident = newIncident(new AiDetection(0.91, 0.1, 0.2, 0.3, 0.4));

        assertThat(incident.status()).isEqualTo(IncidentStatus.PENDING);
        assertThat(incident.aiValidated()).isTrue();
        assertThat(incident.history()).singleElement().satisfies(change -> {
            assertThat(change.fromStatus()).isNull();
            assertThat(change.toStatus()).isEqualTo(IncidentStatus.PENDING);
            assertThat(change.changedBy()).isEqualTo(CITIZEN);
        });
    }

    @Test
    void manualReportHasNoAiDetection() {
        assertThat(newIncident(null).aiValidated()).isFalse();
    }

    @Test
    void followsTheValidLifecycleAndRecordsEachChange() {
        Incident incident = newIncident(null);
        incident.changeStatus(IncidentStatus.IN_PROGRESS, OPERATOR, "Cuadrilla asignada", NOW.plusSeconds(60));
        incident.changeStatus(IncidentStatus.RESOLVED, OPERATOR, null, NOW.plusSeconds(120));

        assertThat(incident.status()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(incident.history()).extracting(StatusChange::toStatus)
                .containsExactly(IncidentStatus.PENDING, IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED);
        assertThat(incident.updatedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectsInvalidTransitions() {
        Incident incident = newIncident(null);
        assertThatThrownBy(() -> incident.changeStatus(IncidentStatus.RESOLVED, OPERATOR, null, NOW))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(incident.history()).hasSize(1);
    }

    @Test
    void rejectionRequiresAReason() {
        Incident incident = newIncident(null);
        assertThatThrownBy(() -> incident.changeStatus(IncidentStatus.REJECTED, OPERATOR, "  ", NOW))
                .isInstanceOf(InvalidIncidentException.class);

        incident.changeStatus(IncidentStatus.REJECTED, OPERATOR, "La foto no muestra la pista", NOW);
        assertThat(incident.history().getLast().comment()).isEqualTo("La foto no muestra la pista");
    }

    @Test
    void severityCanOnlyChangeWhileOpen() {
        Incident incident = newIncident(null);
        incident.assignSeverity(Severity.HIGH, NOW);
        assertThat(incident.severity()).contains(Severity.HIGH);

        incident.changeStatus(IncidentStatus.REJECTED, OPERATOR, "Duplicado", NOW);
        assertThatThrownBy(() -> incident.assignSeverity(Severity.LOW, NOW)).isInstanceOf(InvalidIncidentException.class);
    }

    @Test
    void operatorAssignsAndReassignsAnOpenIncident() {
        Incident incident = newIncident(null);
        assertThat(incident.assignment()).isEmpty();

        incident.assign(" Juan Pérez ", " Obras Públicas ", "  ", OPERATOR, NOW.plusSeconds(60));
        assertThat(incident.assignment()).hasValueSatisfying(assignment -> {
            assertThat(assignment.assignee()).isEqualTo("Juan Pérez");
            assertThat(assignment.area()).isEqualTo("Obras Públicas");
            assertThat(assignment.note()).isNull();
            assertThat(assignment.assignedBy()).isEqualTo(OPERATOR);
            assertThat(assignment.assignedAt()).isEqualTo(NOW.plusSeconds(60));
        });
        assertThat(incident.updatedAt()).isEqualTo(NOW.plusSeconds(60));

        UUID otherOperator = UUID.randomUUID();
        incident.changeStatus(IncidentStatus.IN_PROGRESS, OPERATOR, null, NOW.plusSeconds(90));
        incident.assign("Rosa Díaz", "Mantenimiento vial", "Llevar asfalto en frío", otherOperator, NOW.plusSeconds(120));
        assertThat(incident.assignment()).hasValueSatisfying(assignment -> {
            assertThat(assignment.assignee()).isEqualTo("Rosa Díaz");
            assertThat(assignment.note()).isEqualTo("Llevar asfalto en frío");
            assertThat(assignment.assignedBy()).isEqualTo(otherOperator);
        });
        assertThat(incident.updatedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void assignmentRequiresAssigneeAndAreaWithinLimits() {
        Incident incident = newIncident(null);
        assertThatThrownBy(() -> incident.assign(" ", "Obras", null, OPERATOR, NOW))
                .isInstanceOf(InvalidIncidentException.class);
        assertThatThrownBy(() -> incident.assign("Juan", null, null, OPERATOR, NOW))
                .isInstanceOf(InvalidIncidentException.class);
        assertThatThrownBy(() -> incident.assign("x".repeat(121), "Obras", null, OPERATOR, NOW))
                .isInstanceOf(InvalidIncidentException.class);
        assertThatThrownBy(() -> incident.assign("Juan", "Obras", "x".repeat(501), OPERATOR, NOW))
                .isInstanceOf(InvalidIncidentException.class);
        assertThat(incident.assignment()).isEmpty();
    }

    @Test
    void closedIncidentsCannotBeAssigned() {
        Incident rejected = newIncident(null);
        rejected.changeStatus(IncidentStatus.REJECTED, OPERATOR, "Duplicado", NOW);
        assertThatThrownBy(() -> rejected.assign("Juan", "Obras", null, OPERATOR, NOW))
                .isInstanceOf(IncidentClosedException.class)
                .hasMessage("No se puede asignar una incidencia REJECTED");

        Incident resolved = newIncident(null);
        resolved.assign("Juan", "Obras", null, OPERATOR, NOW);
        resolved.changeStatus(IncidentStatus.IN_PROGRESS, OPERATOR, null, NOW);
        resolved.changeStatus(IncidentStatus.RESOLVED, OPERATOR, null, NOW);
        assertThatThrownBy(() -> resolved.assign("Rosa", "Obras", null, OPERATOR, NOW.plusSeconds(60)))
                .isInstanceOf(IncidentClosedException.class);
        assertThat(resolved.assignment()).map(Assignment::assignee).contains("Juan");
    }

    @Test
    void validatesCoordinatesAndAiValues() {
        assertThatThrownBy(() -> new GeoLocation(91, 0)).isInstanceOf(InvalidIncidentException.class);
        assertThatThrownBy(() -> new GeoLocation(0, -181)).isInstanceOf(InvalidIncidentException.class);
        assertThatThrownBy(() -> new AiDetection(1.2, 0, 0, 0, 0)).isInstanceOf(InvalidIncidentException.class);
    }

    private static Incident newIncident(AiDetection ai) {
        return Incident.register(CITIZEN, UUID.randomUUID(), IncidentType.D40, new GeoLocation(-12.0464, -77.0428),
                null, " Bache grande ", ai, NOW);
    }
}
