package pe.viadeep.auth.presentation;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.viadeep.auth.application.ManageUsersUseCase;
import pe.viadeep.auth.presentation.AuthDtos.OperatorSummary;

/** Directorio de operadores activos: cualquier operador lo usa para asignar una incidencia a un responsable. */
@RestController
@RequestMapping("/auth/operators")
class OperatorDirectoryController {

    private final ManageUsersUseCase manageUsers;

    OperatorDirectoryController(ManageUsersUseCase manageUsers) {
        this.manageUsers = manageUsers;
    }

    @GetMapping
    List<OperatorSummary> activeOperators() {
        return manageUsers.activeOperators().stream().map(OperatorSummary::from).toList();
    }
}
