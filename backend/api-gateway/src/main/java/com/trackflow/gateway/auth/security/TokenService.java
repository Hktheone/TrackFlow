package com.trackflow.gateway.auth.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.trackflow.gateway.auth.domain.AppUser;

/** Issues signed RS256 access tokens carrying the user's name and role. */
@Service
public class TokenService {

    /** Claim holding the user's roles; every service maps it to Spring Security {@code ROLE_*} authorities. */
    public static final String ROLES_CLAIM = "roles";
    public static final String NAME_CLAIM = "name";

    private final JwtEncoder encoder;
    private final SigningKeys signingKeys;
    private final AuthProperties properties;

    public TokenService(JwtEncoder encoder, SigningKeys signingKeys, AuthProperties properties) {
        this.encoder = encoder;
        this.signingKeys = signingKeys;
        this.properties = properties;
    }

    public IssuedToken issue(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.tokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(NAME_CLAIM, user.getDisplayName())
                .claim(ROLES_CLAIM, List.of(user.getRoleName().name()))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(signingKeys.rsaKey().getKeyID())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
