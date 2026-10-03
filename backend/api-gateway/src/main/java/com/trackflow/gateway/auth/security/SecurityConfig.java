package com.trackflow.gateway.auth.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.trackflow.gateway.auth.repository.RevokedTokenRepository;

/**
 * The gateway is the front door: nothing reaches a service without a valid, unrevoked token.
 * Rules here are coarse (who may touch which API at all); order-service and delivery-service
 * re-check the token and apply the fine-grained "is this yours?" rules themselves.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String USER = "USER";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless bearer tokens, no cookies to forge
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // public
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/.well-known/jwks.json", "/actuator/health/**", "/actuator/info", "/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // user administration
                        .requestMatchers("/api/auth/users", "/api/auth/users/**").hasRole(ADMIN)
                        // riders: admins manage the fleet, users pick a rider for their delivery
                        .requestMatchers(HttpMethod.GET, "/api/couriers", "/api/couriers/**").hasAnyRole(ADMIN, USER)
                        .requestMatchers("/api/couriers", "/api/couriers/**").hasRole(ADMIN)
                        // orders: riders never see the order side
                        .requestMatchers("/api/orders", "/api/orders/**").hasAnyRole(ADMIN, USER)
                        // deliveries: every role, filtered per user by delivery-service
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    JwtEncoder jwtEncoder(SigningKeys keys) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(keys.rsaKey())));
    }

    @Bean
    JwtDecoder jwtDecoder(SigningKeys keys, AuthProperties properties, RevokedTokenRepository revokedTokens) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()),
                new RevokedTokenValidator(revokedTokens)));
        return decoder;
    }

    static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenService.ROLES_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt by default, with an {id} prefix so the hashing scheme can be upgraded later.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AuthProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("X-Request-Id"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
