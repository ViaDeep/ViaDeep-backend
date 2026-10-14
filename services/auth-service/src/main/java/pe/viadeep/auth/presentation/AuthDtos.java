package pe.viadeep.auth.presentation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }

    record UserResponse(UUID id, String email, String fullName, String role) {

        static UserResponse from(User user) {
            return new UserResponse(user.id(), user.email(), user.fullName(), user.role().name());
        }
    }
}
