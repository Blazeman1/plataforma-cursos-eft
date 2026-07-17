package com.duocuc.cursos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Configuración de Spring Security — Semana 8 (Sumativa).
 *
 * Securitiza todos los endpoints del sistema de cursos en línea:
 *   - Rol "descarga"  → solo GET /api/cursos/{codigoCurso}/descargar
 *   - Rol "admin"     → todos los demás endpoints de /api/cursos/**
 *   - Sin token       → 401 Unauthorized
 *   - Token sin rol   → 403 Forbidden
 *   - /actuator/health → público (para pipeline CI/CD)
 *
 * Usa jwk-set-uri en vez de issuer-uri por incompatibilidad de Azure AD B2C
 * con el auto-discovery OIDC estándar (ver informe S5, sección 2.4).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.security.expected-issuer}")
    private String expectedIssuer;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> withIssuer    = new JwtIssuerValidator(expectedIssuer);
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> validator     =
                new DelegatingOAuth2TokenValidator<>(List.of(withIssuer, withTimestamp));

        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            String role = jwt.getClaimAsString("extension_consultaRole");
            if (role != null && !role.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Público: health check para pipeline y Docker
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // Rol "descarga": solo puede descargar guías
                .requestMatchers(HttpMethod.GET, "/api/cursos/*/descargar")
                    .hasAnyRole("estudiante", "instructor")
                // Rol "admin": acceso completo al resto de endpoints
                .requestMatchers("/api/cursos/**").hasRole("instructor")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                ));

        return http.build();
    }
}
