package pe.viadeep.incident.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IncidentStatusTest {

    @Test
    void allowsTransitionsDefinedInTheArchitecture() {
        assertThat(IncidentStatus.PENDING.canTransitionTo(IncidentStatus.IN_PROGRESS)).isTrue();
        assertThat(IncidentStatus.PENDING.canTransitionTo(IncidentStatus.REJECTED)).isTrue();
        assertThat(IncidentStatus.IN_PROGRESS.canTransitionTo(IncidentStatus.RESOLVED)).isTrue();
    }

    @Test
    void rejectsAnyOtherTransition() {
        assertThat(IncidentStatus.PENDING.canTransitionTo(IncidentStatus.RESOLVED)).isFalse();
        assertThat(IncidentStatus.IN_PROGRESS.canTransitionTo(IncidentStatus.PENDING)).isFalse();
        assertThat(IncidentStatus.RESOLVED.canTransitionTo(IncidentStatus.IN_PROGRESS)).isFalse();
        assertThat(IncidentStatus.REJECTED.canTransitionTo(IncidentStatus.PENDING)).isFalse();
    }
}
