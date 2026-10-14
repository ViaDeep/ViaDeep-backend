package pe.viadeep.auth.application;

import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public User updateProfile(UUID userId, String fullName, String phone, String documentNumber) {
        return users.save(find(userId).withProfile(fullName, phone, documentNumber));
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
