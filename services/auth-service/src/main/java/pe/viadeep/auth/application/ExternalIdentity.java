package pe.viadeep.auth.application;

/** Correo comprobado por Google o Microsoft y el nombre que muestra esa cuenta (puede venir vacío). */
public record ExternalIdentity(String email, String fullName) {
}
