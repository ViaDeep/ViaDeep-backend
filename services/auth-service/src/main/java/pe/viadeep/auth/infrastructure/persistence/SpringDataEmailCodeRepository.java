package pe.viadeep.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import pe.viadeep.auth.domain.EmailCodePurpose;

interface SpringDataEmailCodeRepository extends JpaRepository<EmailCodeJpaEntity, UUID> {

    Optional<EmailCodeJpaEntity> findFirstByEmailAndPurposeOrderByCreatedAtDesc(String email, EmailCodePurpose purpose);

    @Modifying
    void deleteByEmailAndPurpose(String email, EmailCodePurpose purpose);
}
