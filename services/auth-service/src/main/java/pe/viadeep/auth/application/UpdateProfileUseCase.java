package pe.viadeep.auth.application;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.auth.domain.DocumentNumberAlreadyRegisteredException;
import pe.viadeep.auth.domain.Role;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

/** El usuario edita su propio perfil y su contraseña. */
@Service
public class UpdateProfileUseCase {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;

    public UpdateProfileUseCase(UserRepository users, PasswordHasher passwordHasher) {
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    /**
     * En una cuenta de operador el DNI identifica a la persona (una sola cuenta por DNI): no se puede cambiar ni
     * borrar una vez registrado, y si aún no tiene uno no puede usar el de otro operador.
     */
    @Transactional
    public User updateProfile(UUID userId, String fullName, String phone, String documentNumber) {
        User user = find(userId);
        String requested = documentNumber == null || documentNumber.isBlank() ? null : documentNumber.strip();
        if (user.role() == Role.OPERATOR && !Objects.equals(requested, user.documentNumber().orElse(null))) {
            if (user.documentNumber().isPresent()) {
                throw new OperatorDocumentLockedException();
            }
            if (users.operatorDocumentTaken(requested, user.id())) {
                throw new DocumentNumberAlreadyRegisteredException(requested);
            }
        }
        return users.save(user.withProfile(fullName, phone, documentNumber));
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = find(userId);
        if (!passwordHasher.matches(currentPassword, user.passwordHash())) {
            throw new WrongPasswordException();
        }
        users.save(user.withPasswordHash(passwordHasher.hash(newPassword)));
    }

    private User find(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
    }
}
