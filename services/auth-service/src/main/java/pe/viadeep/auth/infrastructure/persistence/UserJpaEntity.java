package pe.viadeep.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import pe.viadeep.auth.domain.Role;
import pe.viadeep.auth.domain.User;

@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(length = 15)
    private String phone;

    @Column(name = "document_number", length = 8)
    private String documentNumber;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired;

    @Column(nullable = false)
    private boolean administrator;

    protected UserJpaEntity() {
    }

    static UserJpaEntity fromDomain(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.id();
        entity.email = user.email();
        entity.passwordHash = user.passwordHash();
        entity.fullName = user.fullName();
        entity.role = user.role();
        entity.createdAt = user.createdAt();
        entity.phone = user.phone().orElse(null);
        entity.documentNumber = user.documentNumber().orElse(null);
        entity.enabled = user.enabled();
        entity.lastLoginAt = user.lastLoginAt().orElse(null);
        entity.passwordChangeRequired = user.passwordChangeRequired();
        entity.administrator = user.administrator();
        return entity;
    }

    User toDomain() {
        return new User(id, email, passwordHash, fullName, role, createdAt, phone, documentNumber, enabled, lastLoginAt,
                passwordChangeRequired, administrator);
    }
}
