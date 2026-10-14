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

    public User(UUID id, String email, String passwordHash, String fullName, Role role, Instant createdAt,
                String phone, String documentNumber) {
        this.id = Objects.requireNonNull(id);
        this.email = normalizeEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.fullName = Objects.requireNonNull(fullName).strip();
        this.role = Objects.requireNonNull(role);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.phone = blankToNull(phone);
        this.documentNumber = blankToNull(documentNumber);
    }

    public static User register(String email, String passwordHash, String fullName, Role role, Instant now) {
        return new User(UUID.randomUUID(), email, passwordHash, fullName, role, now, null, null);
    }

    /** Datos que el propio usuario puede editar. El correo y el rol no cambian. */
    public User withProfile(String fullName, String phone, String documentNumber) {
        return new User(id, email, passwordHash, fullName, role, createdAt, phone, documentNumber);
    }

    public User withPasswordHash(String passwordHash) {
        return new User(id, email, passwordHash, fullName, role, createdAt, phone, documentNumber);
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

    /** DNI del ciudadano. */
    public Optional<String> documentNumber() {
        return Optional.ofNullable(documentNumber);
    }
}
