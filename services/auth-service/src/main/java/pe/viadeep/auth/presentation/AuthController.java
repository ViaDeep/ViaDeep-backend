package pe.viadeep.auth.presentation;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.viadeep.auth.application.EmailVerificationUseCase;
import pe.viadeep.auth.application.GetCurrentUserUseCase;
import pe.viadeep.auth.application.IdentityProvider;
import pe.viadeep.auth.application.LoginUseCase;
import pe.viadeep.auth.application.PasswordResetUseCase;
import pe.viadeep.auth.application.RegisterUserUseCase;
import pe.viadeep.auth.application.SocialLoginUseCase;
import pe.viadeep.auth.application.UpdateProfileUseCase;
import pe.viadeep.auth.presentation.AuthDtos.ChangePasswordRequest;
import pe.viadeep.auth.presentation.AuthDtos.EmailRequest;
import pe.viadeep.auth.presentation.AuthDtos.LoginRequest;
import pe.viadeep.auth.presentation.AuthDtos.NewOperatorRequest;
import pe.viadeep.auth.presentation.AuthDtos.RegisterRequest;
import pe.viadeep.auth.presentation.AuthDtos.ResetPasswordRequest;
import pe.viadeep.auth.presentation.AuthDtos.SocialLoginRequest;
import pe.viadeep.auth.presentation.AuthDtos.TokenResponse;
import pe.viadeep.auth.presentation.AuthDtos.UpdateProfileRequest;
import pe.viadeep.auth.presentation.AuthDtos.UserResponse;
import pe.viadeep.auth.presentation.AuthDtos.VerifyEmailRequest;

@RestController
@RequestMapping("/auth")
class AuthController {

    private final RegisterUserUseCase registerUser;
    private final LoginUseCase login;
    private final GetCurrentUserUseCase getCurrentUser;
    private final UpdateProfileUseCase updateProfile;
    private final EmailVerificationUseCase emailVerification;
    private final PasswordResetUseCase passwordReset;
    private final SocialLoginUseCase socialLogin;

    AuthController(RegisterUserUseCase registerUser, LoginUseCase login, GetCurrentUserUseCase getCurrentUser,
                   UpdateProfileUseCase updateProfile, EmailVerificationUseCase emailVerification,
                   PasswordResetUseCase passwordReset, SocialLoginUseCase socialLogin) {
        this.registerUser = registerUser;
        this.login = login;
        this.getCurrentUser = getCurrentUser;
        this.updateProfile = updateProfile;
        this.emailVerification = emailVerification;
        this.passwordReset = passwordReset;
        this.socialLogin = socialLogin;
    }

    /** Crea la cuenta pendiente y envía un código de 6 dígitos al correo. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(registerUser.register(request.email(), request.password(), request.fullName()));
    }

    /**
     * Solo el administrador da de alta operadores municipales, con un correo del dominio institucional y el DNI de la
     * persona (una cuenta de operador por DNI). La contraseña es temporal: el nuevo operador debe cambiarla en su
     * primer ingreso.
     */
    @PostMapping("/operators")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse registerOperator(@Valid @RequestBody NewOperatorRequest request) {
        return UserResponse.from(registerUser.inviteOperator(request.email(), request.password(), request.fullName(),
                request.documentNumber()));
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        LoginUseCase.Session session = login.login(request.email(), request.password());
        return new TokenResponse(session.token().value(), "Bearer", session.token().expiresInSeconds(),
                session.passwordChangeRequired());
    }

    /** Entra (o crea la cuenta de ciudadano) con el token de identidad de Google. */
    @PostMapping("/google")
    TokenResponse google(@Valid @RequestBody SocialLoginRequest request) {
        return toResponse(socialLogin.login(IdentityProvider.GOOGLE, request.idToken()));
    }

    /** Igual que Google, con una cuenta personal de Microsoft (Outlook, Hotmail o Live). */
    @PostMapping("/microsoft")
    TokenResponse microsoft(@Valid @RequestBody SocialLoginRequest request) {
        return toResponse(socialLogin.login(IdentityProvider.MICROSOFT, request.idToken()));
    }

    /** Con el código correcto la cuenta queda verificada y se devuelve la sesión, como en el login. */
    @PostMapping("/email/verify")
    TokenResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        LoginUseCase.Session session = emailVerification.verify(request.email(), request.code());
        return new TokenResponse(session.token().value(), "Bearer", session.token().expiresInSeconds(),
                session.passwordChangeRequired());
    }

    @PostMapping("/email/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resendVerification(@Valid @RequestBody EmailRequest request) {
        emailVerification.resend(request.email());
    }

    /** Siempre 204, exista o no la cuenta, para no revelar qué correos están registrados. */
    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void forgotPassword(@Valid @RequestBody EmailRequest request) {
        passwordReset.requestReset(request.email());
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordReset.reset(request.email(), request.code(), request.newPassword());
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(getCurrentUser.get(UUID.fromString(jwt.getSubject())));
    }

    @PutMapping("/me")
    UserResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(updateProfile.updateProfile(UUID.fromString(jwt.getSubject()), request.fullName(),
                request.phone(), request.documentNumber()));
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        updateProfile.changePassword(UUID.fromString(jwt.getSubject()), request.currentPassword(), request.newPassword());
    }

    private static TokenResponse toResponse(LoginUseCase.Session session) {
        return new TokenResponse(session.token().value(), "Bearer", session.token().expiresInSeconds(),
                session.passwordChangeRequired());
    }
}
