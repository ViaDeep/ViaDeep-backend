package pe.viadeep.auth.presentation;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.viadeep.auth.application.GetCurrentUserUseCase;
import pe.viadeep.auth.application.LoginUseCase;
import pe.viadeep.auth.application.RegisterUserUseCase;
import pe.viadeep.auth.application.TokenIssuer;
import pe.viadeep.auth.presentation.AuthDtos.LoginRequest;
import pe.viadeep.auth.presentation.AuthDtos.RegisterRequest;
import pe.viadeep.auth.presentation.AuthDtos.TokenResponse;
import pe.viadeep.auth.presentation.AuthDtos.UserResponse;

@RestController
@RequestMapping("/auth")
class AuthController {

    private final RegisterUserUseCase registerUser;
    private final LoginUseCase login;
    private final GetCurrentUserUseCase getCurrentUser;

    AuthController(RegisterUserUseCase registerUser, LoginUseCase login, GetCurrentUserUseCase getCurrentUser) {
        this.registerUser = registerUser;
        this.login = login;
        this.getCurrentUser = getCurrentUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(registerUser.register(request.email(), request.password(), request.fullName()));
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        TokenIssuer.IssuedToken token = login.login(request.email(), request.password());
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(getCurrentUser.get(UUID.fromString(jwt.getSubject())));
    }
}
