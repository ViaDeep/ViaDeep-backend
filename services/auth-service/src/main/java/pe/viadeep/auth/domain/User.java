package pe.viadeep.auth.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class User {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final Role role;
    private final Instant createdAt;
    private final String phone;
    private final String documentNumber;
    private final boolean enabled;
    private final Instant lastLoginAt;
    private final boolean passwordChangeRequired;
    private final boolean administrator;

    public User(UUID id, String email, String passwordHash, String fullName, Role role, Instant createdAt,
                String phone, String documentNumber, boolean enabled, Instant lastLoginAt,
                boolean passwordChangeRequired, boolean administrator) {
        this.id = Objects.requireNonNull(id);
        this.email = normalizeEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.fullName = Objects.requireNonNull(fullName).strip();
        this.role = Objects.requireNonNull(role);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.phone = blankToNull(phone);
        this.documentNumber = blankToNull(documentNumber);
        this.enabled = enabled;
        this.lastLoginAt = lastLoginAt;
        this.passwordChangeRequired = passwordChangeRequired;
        this.administrator = administrator;
        if (administrator && role != Role.OPERATOR) {
            throw new IllegalArgumentException("Solo un operador puede ser administrador");
        }
    }

    /** Las cuentas nuevas nacen activas y sin inicios de sesión. */
    public static User register(String email, String passwordHash, String fullName, Role role, Instant now) {
        return new User(UUID.randomUUID(), email, passwordHash, fullName, role, now, null, null, true, null, false, false);
    }

    /**
     * Operador dado de alta por el administrador: la contraseña la eligió el administrador, así que es temporal y debe
     * cambiarla en su primer ingreso. El DNI identifica a la persona: no puede tener dos cuentas de operador.
     */
    public static User inviteOperator(String email, String temporaryPasswordHash, String fullName,
                                      String documentNumber, Instant now) {
        return new User(UUID.randomUUID(), email, temporaryPasswordHash, fullName, Role.OPERATOR, now, null,
                Objects.requireNonNull(documentNumber), true, null, true, false);
    }

    /** Datos que el propio usuario puede editar. El correo y el rol no cambian. */
    public User withProfile(String fullName, String phone, String documentNumber) {
        return new User(id, email, passwordHash, fullName, role, createdAt, phone, documentNumber, enabled, lastLoginAt,
                passwordChangeRequired, administrator);
    }

    /** Al elegir su propia contraseña deja de tener una temporal. */
    public User withPasswordHash(String passwordHash) {
        return new User(id, email, passwordHash, fullName, role, createdAt, phone, documentNumber, enabled, lastLoginAt,
                false, administrator);
    }

    /** Un operador activa o desactiva la cuenta; una cuenta desactivada no puede iniciar sesión. */
    public User withEnabled(boolean enabled) {
        return new User(id, email, passwordHash, fullName, role, createdAt, phone, documentNumber, enabled, lastLoginAt,
                passwordChangeRequired, administrator);
    }

    public User withLastLoginAt(Instant lastLoginAt) {
        return new User(id, email, passwordHash, fullName, role, createdAt, phone, documentNumber, enabled, lastLoginAt,
                passwordChangeRequired, administrator);
    }

    /** El administrador (el encargado que designa la municipalidad) es el único que gestiona las cuentas. */
    public User asAdministrator() {
        return new User(id, email, passwordHash, fullName, role, createdAt, phone, documentNumber, enabled, lastLoginAt,
                passwordChangeRequired, true);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public static String normalizeEmail(String email) {
        return Objects.requireNonNull(email).strip().toLowerCase(Locale.ROOT);
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String fullName() {
        return fullName;
    }

    public Role role() {
        return role;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<String> phone() {
        return Optional.ofNullable(phone);
    }

    /** DNI; obligatorio y único en las cuentas de operador creadas por el administrador. */
    public Optional<String> documentNumber() {
        return Optional.ofNullable(documentNumber);
    }

    public boolean enabled() {
        return enabled;
    }

    /** true mientras el usuario tenga la contraseña temporal que le asignaron. */
    public boolean passwordChangeRequired() {
        return passwordChangeRequired;
    }

    public boolean administrator() {
        return administrator;
    }

    /** Último inicio de sesión exitoso; vacío si nunca inició sesión. */
    public Optional<Instant> lastLoginAt() {
        return Optional.ofNullable(lastLoginAt);
    }
}
