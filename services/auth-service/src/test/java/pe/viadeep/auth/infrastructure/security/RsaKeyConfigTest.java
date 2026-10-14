package pe.viadeep.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;

class RsaKeyConfigTest {

    /** Si la clave cambia (por ejemplo, la temporal al reiniciar), el kid también, y los demás servicios la descargan. */
    @Test
    void theKeyIdChangesWithTheKeyAndStaysTheSameForTheSameKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair first = generator.generateKeyPair();
        KeyPair second = generator.generateKeyPair();

        RSAKey a = RsaKeyConfig.withKeyId((RSAPublicKey) first.getPublic(), first.getPrivate(), "viadeep-auth-1");
        RSAKey again = RsaKeyConfig.withKeyId((RSAPublicKey) first.getPublic(), first.getPrivate(), "viadeep-auth-1");
        RSAKey b = RsaKeyConfig.withKeyId((RSAPublicKey) second.getPublic(), second.getPrivate(), "viadeep-auth-1");

        assertThat(a.getKeyID()).startsWith("viadeep-auth-1-").isEqualTo(again.getKeyID()).isNotEqualTo(b.getKeyID());
    }
}
