package pe.viadeep.auth.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RsaKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyConfig.class);

    @Bean
    RSAKey rsaKey(JwtProperties properties) throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        KeyPair keyPair = properties.privateKey() == null || properties.privateKey().isBlank()
                ? generateTemporaryKeyPair()
                : keyPairFromPem(properties.privateKey());
        return withKeyId((RSAPublicKey) keyPair.getPublic(), keyPair.getPrivate(), properties.keyId());
    }

    /**
     * El kid lleva una huella de la clave pública. Así, si la clave cambia (la temporal de desarrollo cambia en cada
     * reinicio), los demás servicios ven un kid nuevo y vuelven a descargar el JWKS en ese momento, en vez de seguir
     * validando con la clave anterior que tienen en caché y rechazar los tokens nuevos con 401.
     */
    static RSAKey withKeyId(RSAPublicKey publicKey, PrivateKey privateKey, String keyIdPrefix) throws JOSEException {
        String thumbprint = new RSAKey.Builder(publicKey).build().computeThumbprint().toString();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(keyIdPrefix + "-" + thumbprint.substring(0, 8))
                .build();
    }

    @Bean
    JWKSet publicJwkSet(RSAKey rsaKey) {
        return new JWKSet(rsaKey.toPublicJWK());
    }

    private static KeyPair generateTemporaryKeyPair() throws NoSuchAlgorithmException {
        log.warn("VIADEEP_JWT_PRIVATE_KEY no está configurada: se usa una clave RSA temporal (solo desarrollo).");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static KeyPair keyPairFromPem(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String base64 = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
        KeyFactory factory = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) factory.generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
        return new KeyPair(publicKey, privateKey);
    }
}
