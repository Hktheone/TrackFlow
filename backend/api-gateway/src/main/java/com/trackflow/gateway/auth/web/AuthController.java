package com.trackflow.gateway.auth.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trackflow.gateway.auth.service.AuthService;
import com.trackflow.gateway.auth.web.dto.AuthResponse;
import com.trackflow.gateway.auth.web.dto.LoginRequest;
import com.trackflow.gateway.auth.web.dto.RegisterRequest;
import com.trackflow.gateway.auth.web.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.username(), request.password(), request.displayName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt token) {
        authService.logout(token);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt token) {
        return authService.currentUser(token.getSubject());
    }
}
