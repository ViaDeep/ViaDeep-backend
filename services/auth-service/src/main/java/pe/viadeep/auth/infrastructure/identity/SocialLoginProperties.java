package pe.viadeep.auth.infrastructure.identity;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Identificadores de la app en Google Cloud y en Microsoft Entra. Sin ellos, ese botón responde 503.
 *
 * @param googleClientIds IDs de cliente OAuth de Google cuyo token se acepta (el de tipo "Aplicación web" que usa la
 *                        app para pedir el token), separados por comas
 * @param microsoftClientId ID de aplicación (cliente) registrado en Microsoft Entra para cuentas personales
 */
@ConfigurationProperties(prefix = "viadeep.social")
public record SocialLoginProperties(List<String> googleClientIds, String microsoftClientId) {

    public SocialLoginProperties {
        googleClientIds = googleClientIds == null ? List.of()
                : googleClientIds.stream().map(String::strip).filter(id -> !id.isEmpty()).toList();
        microsoftClientId = microsoftClientId == null || microsoftClientId.isBlank() ? null : microsoftClientId.strip();
    }
}
