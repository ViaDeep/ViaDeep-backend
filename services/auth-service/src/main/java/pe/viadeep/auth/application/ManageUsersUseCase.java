package pe.viadeep.auth.application;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.auth.domain.Role;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

/** Administración de usuarios desde la plataforma web: listado, detalle y activación o desactivación de cuentas. */
@Service
public class ManageUsersUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    /** Tope de la lista de operadores para asignar incidencias; una municipalidad tiene pocas decenas. */
    private static final int MAX_OPERATORS = 500;

    private final UserRepository users;
    private final UserQueryRepository queries;

    public ManageUsersUseCase(UserRepository users, UserQueryRepository queries) {
        this.users = users;
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public PageResult<User> search(UserFilter filter, int page, int size) {
        return queries.search(filter, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public User get(UUID userId) {
        return find(userId);
    }

    /** Operadores activos, para elegir al responsable de una incidencia. */
    @Transactional(readOnly = true)
    public List<User> activeOperators() {
        return queries.search(new UserFilter(Role.OPERATOR, true, null), 0, MAX_OPERATORS).items();
    }

    /**
     * El administrador corrige el nombre y el celular de una cuenta. El correo (con el que inicia sesión) y el DNI (que
     * identifica a la persona) no se cambian desde aquí.
     */
    @Transactional
    public User updateBasicInfo(UUID userId, String fullName, String phone) {
        User user = find(userId);
        String cleanPhone = phone == null || phone.isBlank() ? null : phone.strip();
        return users.save(user.withProfile(fullName.strip(), cleanPhone, user.documentNumber().orElse(null)));
    }

    /** Un operador no puede desactivar su propia cuenta, para no quedarse sin acceso a la plataforma. */
    @Transactional
    public User changeStatus(UUID userId, boolean enabled, UUID operatorId) {
        if (!enabled && userId.equals(operatorId)) {
            throw new CannotDisableOwnAccountException();
        }
        User user = find(userId);
        return user.enabled() == enabled ? user : users.save(user.withEnabled(enabled));
    }

    /**
     * Un administrador da o quita el permiso de administrador a otro operador activo. Nadie puede quitarse el suyo, así
     * que siempre queda al menos un administrador (quien hace el cambio). Un administrador tampoco puede desactivarse a
     * sí mismo, por la misma razón.
     */
    @Transactional
    public User changeAdministrator(UUID userId, boolean administrator, UUID adminId) {
        if (!administrator && userId.equals(adminId)) {
            throw new CannotRemoveOwnAdministratorException();
        }
        User user = find(userId);
        if (user.administrator() == administrator) {
            return user;
        }
        if (administrator && (user.role() != Role.OPERATOR || !user.enabled())) {
            throw new AdministratorNotAllowedException();
        }
        return users.save(user.withAdministrator(administrator));
    }

    private User find(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
    }
}
