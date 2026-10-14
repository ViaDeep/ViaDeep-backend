package pe.viadeep.auth.application;

import pe.viadeep.auth.domain.Role;

/** Filtros del listado de usuarios; los null no filtran. text busca en el nombre o el correo, sin distinguir mayúsculas. */
public record UserFilter(Role role, Boolean enabled, String text) {
}
