package com.trackflow.gateway.auth.security;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param issuer          value of the {@code iss} claim; services reject tokens from anyone else
 * @param tokenTtl        how long an access token stays valid
 * @param allowedOrigins  browser origins allowed to call the API cross-origin (the Angular dev server)
 * @param seedDemoUsers   create the demo accounts on first start when the users table is empty
 * @param demoPassword    password given to every demo account
 */
@ConfigurationProperties("trackflow.auth")
public record AuthProperties(
        String issuer,
        Duration tokenTtl,
        List<String> allowedOrigins,
        boolean seedDemoUsers,
        String demoPassword) {
}
