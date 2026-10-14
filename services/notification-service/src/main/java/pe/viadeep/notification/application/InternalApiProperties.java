package pe.viadeep.notification.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clave compartida con el servicio de incidencias para crear avisos (POST /notifications). Esa ruta no se expone en el
 * API Gateway; la clave evita que un usuario cree avisos si llega directo al servicio.
 */
@ConfigurationProperties(prefix = "viadeep.internal-api")
public record InternalApiProperties(String token) {
}
