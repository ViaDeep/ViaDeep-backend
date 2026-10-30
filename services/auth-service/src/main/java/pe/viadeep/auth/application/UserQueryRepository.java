package pe.viadeep.auth.application;

import pe.viadeep.auth.domain.User;

/** Consultas del listado de usuarios para los operadores. */
public interface UserQueryRepository {

    /** Usuarios que cumplen el filtro, del más reciente al más antiguo. */
    PageResult<User> search(UserFilter filter, int page, int size);
}
