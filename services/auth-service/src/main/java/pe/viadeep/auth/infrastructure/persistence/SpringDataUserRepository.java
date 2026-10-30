package pe.viadeep.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.viadeep.auth.domain.Role;

interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID>, JpaSpecificationExecutor<UserJpaEntity> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRoleAndDocumentNumber(Role role, String documentNumber);

    boolean existsByRoleAndDocumentNumberAndIdNot(Role role, String documentNumber, UUID id);
}
