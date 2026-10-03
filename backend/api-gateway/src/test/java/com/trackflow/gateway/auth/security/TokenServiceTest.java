package com.trackflow.gateway.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.trackflow.gateway.auth.domain.AppUser;
import com.trackflow.gateway.auth.domain.RoleName;
import com.trackflow.gateway.auth.repository.RevokedTokenRepository;
import com.trackflow.gateway.auth.repository.SigningKeyRepository;

class TokenServiceTest {

    private static final AuthProperties PROPERTIES =
            new AuthProperties("trackflow-gateway", Duration.ofMinutes(5), List.of(), false, "unused");

    private final SecurityConfig config = new SecurityConfig();
    private final RevokedTokenRepository revoked = mock(RevokedTokenRepository.class);
    private SigningKeys keys;
    private TokenService tokens;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        keys = newKeys();
        tokens = new TokenService(config.jwtEncoder(keys), keys, PROPERTIES);
        decoder = config.jwtDecoder(keys, PROPERTIES, revoked);
        when(revoked.existsById(anyString())).thenReturn(false);
    }

    @Test
    void issuedTokenCarriesUsernameRoleAndIssuer() {
        Jwt jwt = decoder.decode(tokens.issue(user("grace", RoleName.USER)).value());

        assertThat(jwt.getSubject()).isEqualTo("grace");
        assertThat(jwt.getClaimAsStringList(TokenService.ROLES_CLAIM)).containsExactly("USER");
        assertThat(jwt.getClaimAsString(TokenService.NAME_CLAIM)).isEqualTo("Grace Hopper");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("trackflow-gateway");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getHeaders()).containsEntry("kid", keys.rsaKey().getKeyID());
    }

    @Test
    void revokedTokenIsRejected() {
        String token = tokens.issue(user("amara", RoleName.RIDER)).value();
        String jti = decoder.decode(token).getId();
        when(revoked.existsById(jti)).thenReturn(true);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        SigningKeys otherKeys = newKeys();
        String forged = new TokenService(config.jwtEncoder(otherKeys), otherKeys, PROPERTIES)
                .issue(user("mallory", RoleName.ADMIN)).value();

        assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        AuthProperties otherIssuer = new AuthProperties("someone-else", Duration.ofMinutes(5), List.of(), false, "x");
        String token = new TokenService(config.jwtEncoder(keys), keys, otherIssuer)
                .issue(user("grace", RoleName.USER)).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private static SigningKeys newKeys() {
        SigningKeyRepository repository = mock(SigningKeyRepository.class);
        when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        return new SigningKeys(repository);
    }

    private static AppUser user(String username, RoleName role) {
        AppUser user = mock(AppUser.class);
        when(user.getUsername()).thenReturn(username);
        when(user.getDisplayName()).thenReturn(username.equals("grace") ? "Grace Hopper" : username);
        when(user.getRoleName()).thenReturn(role);
        return user;
    }
}
