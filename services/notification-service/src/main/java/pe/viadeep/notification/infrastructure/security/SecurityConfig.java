package pe.viadeep.notification.infrastructure.security;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import pe.viadeep.notification.application.InternalApiProperties;

/**
 * Valida el JWT de cada solicitud con la clave pública (JWKS) del servicio de autenticación. POST /notifications es la
 * ruta interna del servicio de incidencias: no lleva JWT y la protege la clave interna (InternalNotificationController).
 */
@Configuration
@EnableConfigurationProperties(InternalApiProperties.class)
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/notifications").permitAll()
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
    Clock clock() {
        return Clock.systemUTC();
    }
}
