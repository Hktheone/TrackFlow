package com.trackflow.order.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Verifies the gateway-issued JWT on every request (signature via the gateway's JWKS, plus issuer and
 * expiry). This is defence in depth: even a request that bypassed the gateway needs a valid token.
 * Role checks are on the controller methods; ownership checks are in {@code OrderService}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /error must be reachable or a failing request is reported as 401 instead of its real status
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> { }));
        return http.build();
    }
}
