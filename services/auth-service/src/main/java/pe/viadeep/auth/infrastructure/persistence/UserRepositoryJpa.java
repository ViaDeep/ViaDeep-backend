package pe.viadeep.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pe.viadeep.auth.domain.Role;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

@Repository
class UserRepositoryJpa implements UserRepository {

    private final SpringDataUserRepository jpa;

    UserRepositoryJpa(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        return jpa.save(UserJpaEntity.fromDomain(user)).toDomain();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public boolean operatorDocumentTaken(String documentNumber, UUID exceptUserId) {
        return exceptUserId == null
                ? jpa.existsByRoleAndDocumentNumber(Role.OPERATOR, documentNumber)
                : jpa.existsByRoleAndDocumentNumberAndIdNot(Role.OPERATOR, documentNumber, exceptUserId);
    }
}
