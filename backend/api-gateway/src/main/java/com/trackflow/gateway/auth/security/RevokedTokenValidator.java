package com.trackflow.gateway.auth.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.trackflow.gateway.auth.repository.RevokedTokenRepository;

/** Rejects tokens whose owner has logged out, even though their signature and expiry are still valid. */
class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED = new OAuth2Error("invalid_token", "Token has been revoked", null);

    private final RevokedTokenRepository revokedTokens;

    RevokedTokenValidator(RevokedTokenRepository revokedTokens) {
        this.revokedTokens = revokedTokens;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String jti = token.getId();
        return jti != null && revokedTokens.existsById(jti)
                ? OAuth2TokenValidatorResult.failure(REVOKED)
                : OAuth2TokenValidatorResult.success();
    }
}
