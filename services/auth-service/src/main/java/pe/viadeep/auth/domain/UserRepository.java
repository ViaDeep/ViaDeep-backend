package pe.viadeep.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);

    /** true si otra cuenta de operador (distinta de exceptUserId) ya usa ese DNI. */
    boolean operatorDocumentTaken(String documentNumber, UUID exceptUserId);
}
