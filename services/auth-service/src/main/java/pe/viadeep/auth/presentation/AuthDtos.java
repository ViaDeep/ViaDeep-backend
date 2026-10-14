package pe.viadeep.auth.presentation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import pe.viadeep.auth.domain.User;

final class AuthDtos {

    private AuthDtos() {
    }

    record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 120) String fullName) {
    }

    record UpdateProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Pattern(regexp = "^$|^9\\d{8}$", message = "debe ser un celular de 9 dígitos que empiece con 9") String phone,
            @Pattern(regexp = "^$|^\\d{8}$", message = "debe tener 8 dígitos") String documentNumber) {
    }

    record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    /** passwordChangeRequired es true si la cuenta tiene una contraseña temporal que debe cambiarse ahora. */
    record TokenResponse(String accessToken, String tokenType, long expiresIn, boolean passwordChangeRequired) {
    }

    record ChangeUserStatusRequest(@NotNull Boolean enabled) {
    }

    /** lastLoginAt es null si el usuario nunca inició sesión. */
    record UserResponse(UUID id, String email, String fullName, String role, String phone, String documentNumber,
                        boolean enabled, Instant createdAt, Instant lastLoginAt, boolean passwordChangeRequired) {

        static UserResponse from(User user) {
            return new UserResponse(user.id(), user.email(), user.fullName(), user.role().name(),
                    user.phone().orElse(null), user.documentNumber().orElse(null), user.enabled(), user.createdAt(),
                    user.lastLoginAt().orElse(null), user.passwordChangeRequired());
        }
    }
}
