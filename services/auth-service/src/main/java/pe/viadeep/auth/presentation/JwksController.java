package pe.viadeep.auth.presentation;

import com.nimbusds.jose.jwk.JWKSet;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publica la clave pública con la que incidencias, evidencias y notificaciones validan los JWT. */
@RestController
class JwksController {

    private final JWKSet publicJwkSet;

    JwksController(JWKSet publicJwkSet) {
        this.publicJwkSet = publicJwkSet;
    }

    @GetMapping("/auth/.well-known/jwks.json")
    Map<String, Object> jwks() {
        return publicJwkSet.toJSONObject();
    }
}
