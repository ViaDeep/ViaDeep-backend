package pe.viadeep.auth.presentation;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.viadeep.auth.application.ManageUsersUseCase;
import pe.viadeep.auth.application.PageResult;
import pe.viadeep.auth.application.UserFilter;
import pe.viadeep.auth.domain.Role;
import pe.viadeep.auth.presentation.AuthDtos.ChangeUserStatusRequest;
import pe.viadeep.auth.presentation.AuthDtos.UserResponse;

/** Administración de usuarios para los operadores municipales (plataforma web). */
@RestController
@RequestMapping("/auth/users")
class UserAdminController {

    private final ManageUsersUseCase manageUsers;

    UserAdminController(ManageUsersUseCase manageUsers) {
        this.manageUsers = manageUsers;
    }

    /** Lista paginada, de la cuenta más reciente a la más antigua. q busca en el nombre o el correo. */
    @GetMapping
    PageResult<UserResponse> search(@RequestParam(required = false) Role role,
                                    @RequestParam(required = false) Boolean enabled,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return manageUsers.search(new UserFilter(role, enabled, q), page, size).map(UserResponse::from);
    }

    @GetMapping("/{id}")
    UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(manageUsers.get(id));
    }

    @PatchMapping("/{id}/status")
    UserResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeUserStatusRequest request,
                              @AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(manageUsers.changeStatus(id, request.enabled(), UUID.fromString(jwt.getSubject())));
    }
}
