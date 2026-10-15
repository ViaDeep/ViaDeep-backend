package pe.viadeep.incident.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reglas para la descripción que escribe el ciudadano: debe hablar del daño en la vía, sin insultos ni groserías y sin
 * enlaces, correos ni números de teléfono. Es un filtro por lista de palabras: atrapa lo evidente (también con tildes,
 * mayúsculas, letras repetidas o números en lugar de letras), pero no entiende el sentido del texto; lo que pase el filtro
 * lo sigue revisando el operador.
 */
public final class DescriptionPolicy {

    /** Groserías e insultos comunes en Perú. Se comparan palabras completas ya normalizadas (sin tildes ni repeticiones). */
    private static final Set<String> BLOCKED_WORDS = Set.of(
            "mierda", "mierdas", "mrd", "carajo", "carajos", "puta", "putas", "puto", "putos", "putamadre", "ptm",
            "conchatumare", "conchatumadre", "conchasumare", "conchasumadre", "ctm", "csm", "chucha", "chuchatumare",
            "huevon", "huevona", "huevones", "huevonas", "webon", "webona", "webones", "weon", "cojudo", "cojuda",
            "cojudos", "cojudas", "cojudez", "cabron", "cabrona", "cabrones", "pendejo", "pendeja", "pendejos",
            "imbecil", "imbeciles", "idiota", "idiotas", "estupido", "estupida", "estupidos", "estupidas", "tarado",
            "tarada", "mongol", "mongolico", "mongolica", "retrasado", "retrasada", "maricon", "maricones", "marica",
            "cabro", "cabros", "rosquete", "zorra", "zorras", "verga", "vergas", "pinga", "pingas",
            "pichula", "cagada", "cagar", "cagon", "culo", "culos", "coño", "joder", "jodido", "jodida", "malparido",
            "malparida", "gonorrea", "hijueputa", "hdp", "hdlgp", "chupala", "chupame", "lameculos");

    /** Enlaces, correos y teléfonos: no aportan a la descripción del daño y suelen ser publicidad o datos personales. */
    private static final Pattern LINK = Pattern.compile("(https?://|www\\.|\\b[\\w-]+\\.(com|pe|net|org)\\b)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    private static final Pattern PHONE = Pattern.compile("(\\d[\\s-]?){9,}");

    private DescriptionPolicy() {
    }

    /** El motivo por el que la descripción no se acepta, o vacío si se puede guardar. */
    public static Optional<String> problem(String description) {
        if (description == null || description.isBlank()) {
            return Optional.empty();
        }
        if (LINK.matcher(description).find() || EMAIL.matcher(description).find()) {
            return Optional.of("La descripción no puede incluir enlaces ni correos. Describe solo el daño en la vía.");
        }
        if (PHONE.matcher(description).find()) {
            return Optional.of("La descripción no puede incluir números de teléfono. Describe solo el daño en la vía.");
        }
        boolean offensive = Arrays.stream(normalize(description).split("[^a-zñ]+")).anyMatch(BLOCKED_WORDS::contains);
        if (offensive) {
            return Optional.of("La descripción contiene palabras inapropiadas. Describe solo el daño en la vía.");
        }
        return Optional.empty();
    }

    /** Minúsculas, sin tildes, con 4→a, 3→e, 1→i, 0→o, @→a, $/5→s y sin letras repetidas ("PUUUTA" → "puta"). */
    static String normalize(String text) {
        String lower = text.toLowerCase().replace('ñ', '\u0001');
        String withoutAccents = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('\u0001', 'ñ');
        String letters = withoutAccents.replace('4', 'a').replace('@', 'a').replace('3', 'e').replace('1', 'i')
                .replace('0', 'o').replace('5', 's').replace('$', 's');
        return letters.replaceAll("([a-zñ])\\1+", "$1");
    }
}
