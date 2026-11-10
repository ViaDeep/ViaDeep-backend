package pe.viadeep.auth.infrastructure.persistence;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import pe.viadeep.auth.domain.EmailCode;
import pe.viadeep.auth.domain.EmailCodePurpose;
import pe.viadeep.auth.domain.EmailCodeRepository;

@Repository
class EmailCodeRepositoryJpa implements EmailCodeRepository {

    private final SpringDataEmailCodeRepository jpa;

    EmailCodeRepositoryJpa(SpringDataEmailCodeRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public EmailCode save(EmailCode code) {
        return jpa.save(EmailCodeJpaEntity.fromDomain(code)).toDomain();
    }

    @Override
    public Optional<EmailCode> findLatest(String email, EmailCodePurpose purpose) {
        return jpa.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, purpose).map(EmailCodeJpaEntity::toDomain);
    }

    @Override
    public void deleteAll(String email, EmailCodePurpose purpose) {
        jpa.deleteByEmailAndPurpose(email, purpose);
        jpa.flush();
    }
}
