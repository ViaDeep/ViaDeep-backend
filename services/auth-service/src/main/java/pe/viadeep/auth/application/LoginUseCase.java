package pe.viadeep.auth.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

@Service
public class LoginUseCase {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public LoginUseCase(UserRepository users, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Transactional(readOnly = true)
    public TokenIssuer.IssuedToken login(String email, String rawPassword) {
        User user = users.findByEmail(User.normalizeEmail(email))
                .filter(found -> passwordHasher.matches(rawPassword, found.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return tokenIssuer.issue(user);
    }
}
