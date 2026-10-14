package pe.viadeep.incident.infrastructure.geocoding;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Distritos de Lima Metropolitana y del Callao con su nombre oficial. OpenStreetMap no siempre pone el distrito en
 * el mismo campo de la dirección, así que se reconoce por el nombre, incluidas las formas abreviadas más comunes.
 */
final class LimaDistricts {

    private static final List<String> OFFICIAL = List.of(
            "Ancón", "Ate", "Barranco", "Breña", "Carabayllo", "Chaclacayo", "Chorrillos", "Cieneguilla", "Comas",
            "El Agustino", "Independencia", "Jesús María", "La Molina", "La Victoria", "Lima", "Lince", "Los Olivos",
            "Lurigancho", "Lurín", "Magdalena del Mar", "Miraflores", "Pachacámac", "Pucusana", "Pueblo Libre",
            "Puente Piedra", "Punta Hermosa", "Punta Negra", "Rímac", "San Bartolo", "San Borja", "San Isidro",
            "San Juan de Lurigancho", "San Juan de Miraflores", "San Luis", "San Martín de Porres", "San Miguel",
            "Santa Anita", "Santa María del Mar", "Santa Rosa", "Santiago de Surco", "Surquillo", "Villa El Salvador",
            "Villa María del Triunfo",
            // Callao
            "Callao", "Bellavista", "Carmen de la Legua Reynoso", "La Perla", "La Punta", "Mi Perú", "Ventanilla");

    private static final Map<String, String> ALIASES = Map.of(
            "Cercado de Lima", "Lima",
            "Lima Cercado", "Lima",
            "Surco", "Santiago de Surco",
            "Magdalena", "Magdalena del Mar",
            "Lurigancho-Chosica", "Lurigancho",
            "Chosica", "Lurigancho",
            "San Martin de Porras", "San Martín de Porres");

    private static final Map<String, String> BY_KEY = new HashMap<>();

    static {
        OFFICIAL.forEach(name -> BY_KEY.put(key(name), name));
        ALIASES.forEach((alias, name) -> BY_KEY.put(key(alias), name));
    }

    private LimaDistricts() {
    }

    /** Nombre oficial del distrito si el texto corresponde a uno ("Distrito de Miraflores" → "Miraflores"). */
    static Optional<String> match(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String key = key(text).replaceFirst("^distrito (de )?", "");
        return Optional.ofNullable(BY_KEY.get(key));
    }

    private static String key(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase().replace('-', ' ').replaceAll("\\s+", " ").strip();
    }
}
