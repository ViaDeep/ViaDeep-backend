package pe.viadeep.auth.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.JOSEException;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/.well-known/jwks.json", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/operators").hasRole("OPERATOR")
                        .requestMatchers("/auth/users", "/auth/users/**").hasRole("OPERATOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter())))
                .build();
    }

    /** Convierte el claim "role" del token (CITIZEN u OPERATOR) en la autoridad ROLE_CITIZEN o ROLE_OPERATOR. */
    private static JwtAuthenticationConverter roleConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
    }

    /** El propio servicio valida sus tokens (para /auth/me) con la misma clave pública que publica en JWKS. */
    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey, JwtProperties properties) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) rsaKey.toPublicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
