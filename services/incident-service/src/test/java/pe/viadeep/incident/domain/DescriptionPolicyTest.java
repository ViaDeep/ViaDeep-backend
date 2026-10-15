package pe.viadeep.incident.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DescriptionPolicyTest {

    @Test
    void acceptsDescriptionsAboutTheRoad() {
        assertThat(DescriptionPolicy.problem("Bache grande frente al paradero de la Av. Arequipa 1200, ocupa el carril derecho"))
                .isEmpty();
        assertThat(DescriptionPolicy.problem("Hay basura y el perro casi se cae en la computadora... digo, la pista"))
                .isEmpty();
        assertThat(DescriptionPolicy.problem(null)).isEmpty();
        assertThat(DescriptionPolicy.problem("   ")).isEmpty();
    }

    @Test
    void rejectsInsultsEvenWhenDisguised() {
        assertThat(DescriptionPolicy.problem("Arreglen esta mierda")).hasValueSatisfying(
                reason -> assertThat(reason).contains("palabras inapropiadas"));
        assertThat(DescriptionPolicy.problem("PUUUTA que bache")).isPresent();
        assertThat(DescriptionPolicy.problem("son unos c0jud0s")).isPresent();
        assertThat(DescriptionPolicy.problem("Huevón el que lo dejó así")).isPresent();
    }

    @Test
    void rejectsLinksEmailsAndPhoneNumbers() {
        assertThat(DescriptionPolicy.problem("mira https://ejemplo.com")).hasValueSatisfying(
                reason -> assertThat(reason).contains("enlaces"));
        assertThat(DescriptionPolicy.problem("visita www.ofertas.pe")).isPresent();
        assertThat(DescriptionPolicy.problem("escríbeme a vecino@correo.pe")).isPresent();
        assertThat(DescriptionPolicy.problem("llámame al 987 654 321")).hasValueSatisfying(
                reason -> assertThat(reason).contains("teléfono"));
    }

    @Test
    void anIncidentWithAnInappropriateDescriptionIsNotRegistered() {
        assertThatThrownBy(() -> Incident.register(UUID.randomUUID(), UUID.randomUUID(), IncidentType.D40,
                new GeoLocation(-12.05, -77.04), null, "qué mierda de pista", null, Instant.now()))
                .isInstanceOf(InappropriateDescriptionException.class);
    }
}
