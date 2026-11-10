package pe.viadeep.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import pe.viadeep.auth.domain.EmailCode;
import pe.viadeep.auth.domain.EmailCodePurpose;

@Entity
@Table(name = "email_codes")
class EmailCodeJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailCodePurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    protected EmailCodeJpaEntity() {
    }

    static EmailCodeJpaEntity fromDomain(EmailCode code) {
        EmailCodeJpaEntity entity = new EmailCodeJpaEntity();
        entity.id = code.id();
        entity.email = code.email();
        entity.purpose = code.purpose();
        entity.codeHash = code.codeHash();
        entity.createdAt = code.createdAt();
        entity.expiresAt = code.expiresAt();
        entity.attempts = code.attempts();
        return entity;
    }

    EmailCode toDomain() {
        return new EmailCode(id, email, purpose, codeHash, createdAt, expiresAt, attempts);
    }
}
