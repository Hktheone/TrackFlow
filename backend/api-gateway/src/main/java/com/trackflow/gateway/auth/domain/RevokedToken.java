package com.trackflow.gateway.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @Column(length = 64)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected RevokedToken() {
    }

    public RevokedToken(String jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }
}
