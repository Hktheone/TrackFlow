package com.trackflow.gateway.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trackflow.gateway.auth.domain.AppUser;
import com.trackflow.gateway.auth.domain.RevokedToken;
import com.trackflow.gateway.auth.domain.Role;
import com.trackflow.gateway.auth.domain.RoleName;
import com.trackflow.gateway.auth.repository.RevokedTokenRepository;
import com.trackflow.gateway.auth.repository.RoleRepository;
import com.trackflow.gateway.auth.repository.UserRepository;
import com.trackflow.gateway.auth.security.TokenService;
import com.trackflow.gateway.auth.service.AuthExceptions.InvalidCredentialsException;
import com.trackflow.gateway.auth.service.AuthExceptions.InvalidUserChangeException;
import com.trackflow.gateway.auth.service.AuthExceptions.UserNotFoundException;
import com.trackflow.gateway.auth.service.AuthExceptions.UsernameTakenException;
import com.trackflow.gateway.auth.web.dto.AuthResponse;
import com.trackflow.gateway.auth.web.dto.UserResponse;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final RevokedTokenRepository revokedTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    /** Compared against when the username doesn't exist, so a miss takes as long as a wrong password. */
    private final String dummyHash;

    public AuthService(UserRepository users, RoleRepository roles, RevokedTokenRepository revokedTokens,
                       PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.roles = roles;
        this.revokedTokens = revokedTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String username, String password) {
        AppUser user = users.findByUsername(normalize(username)).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean matches = passwordEncoder.matches(password, hash);
        if (user == null || !matches || !user.isEnabled()) {
            log.info("Failed login for '{}'", username);
            throw new InvalidCredentialsException();
        }
        log.info("User '{}' logged in as {}", user.getUsername(), user.getRoleName());
        return respond(user);
    }

    /** Self-service sign-up always creates a USER; only an admin can grant ADMIN or RIDER. */
    @Transactional
    public AuthResponse register(String username, String password, String displayName) {
        AppUser user = createUser(username, password, displayName, RoleName.USER);
        return respond(user);
    }

    /** Revokes this token; the gateway refuses it from now until it would have expired anyway. */
    @Transactional
    public void logout(Jwt token) {
        if (token.getId() != null && token.getExpiresAt() != null) {
            revokedTokens.save(new RevokedToken(token.getId(), token.getExpiresAt()));
            log.info("User '{}' logged out", token.getSubject());
        }
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String username) {
        return users.findByUsername(username)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    // ---- administration ----

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return users.findAllByOrderByUsernameAsc().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse createUserAsAdmin(String username, String password, String displayName, RoleName role) {
        return UserResponse.from(createUser(username, password, displayName, role));
    }

    @Transactional
    public UserResponse updateUser(UUID id, RoleName role, Boolean enabled, String actingUsername) {
        AppUser user = users.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        if (user.getUsername().equals(actingUsername)) {
            // Stops an admin from accidentally locking everyone (including themselves) out.
            throw new InvalidUserChangeException("You can't change your own role or disable your own account");
        }
        if (role != null) {
            user.changeRole(role(role));
        }
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        log.info("Admin '{}' updated user '{}': role={}, enabled={}", actingUsername, user.getUsername(),
                user.getRoleName(), user.isEnabled());
        return UserResponse.from(user);
    }

    @Transactional
    public AppUser createUser(String username, String password, String displayName, RoleName role) {
        String normalized = normalize(username);
        if (users.existsByUsername(normalized)) {
            throw new UsernameTakenException(normalized);
        }
        AppUser user = users.save(new AppUser(normalized, passwordEncoder.encode(password), displayName.trim(), role(role)));
        log.info("Created {} account '{}'", role, normalized);
        return user;
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void purgeExpiredRevocations() {
        int removed = revokedTokens.deleteExpired(Instant.now());
        if (removed > 0) {
            log.debug("Purged {} expired revoked tokens", removed);
        }
    }

    private AuthResponse respond(AppUser user) {
        TokenService.IssuedToken token = tokenService.issue(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), UserResponse.from(user));
    }

    private Role role(RoleName name) {
        return roles.findById(name).orElseThrow(() -> new IllegalStateException("Role " + name + " is not seeded"));
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
