package com.trackflow.gateway.auth.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trackflow.gateway.auth.service.AuthService;
import com.trackflow.gateway.auth.web.dto.CreateUserRequest;
import com.trackflow.gateway.auth.web.dto.UpdateUserRequest;
import com.trackflow.gateway.auth.web.dto.UserResponse;

import jakarta.validation.Valid;

/** ADMIN only; enforced in {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/auth/users")
public class UserAdminController {

    private final AuthService authService;

    public UserAdminController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return authService.listUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return authService.createUserAsAdmin(request.username(), request.password(), request.displayName(),
                request.role());
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @RequestBody UpdateUserRequest request,
                               @AuthenticationPrincipal Jwt admin) {
        return authService.updateUser(id, request.role(), request.enabled(), admin.getSubject());
    }
}
