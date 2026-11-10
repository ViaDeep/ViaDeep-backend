package pe.viadeep.auth.application;

/**
 * Comprueba el token de identidad que la app recibió de Google o Microsoft: la firma, el emisor, que fue emitido para
 * ViaDeep, que no venció y que el correo está verificado por ese proveedor.
 */
public interface ExternalIdentityVerifier {

    /**
     * @throws InvalidExternalTokenException si el token no es válido o no trae un correo verificado
     * @throws SocialLoginUnavailableException si ese proveedor no está configurado en el servidor
     */
    ExternalIdentity verify(IdentityProvider provider, String idToken);
}
