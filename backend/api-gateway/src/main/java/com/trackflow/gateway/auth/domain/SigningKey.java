package com.trackflow.gateway.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A Base64-encoded RSA key pair used to sign access tokens; {@code kid} is published in the JWKS. */
@Entity
@Table(name = "signing_keys")
public class SigningKey {

    @Id
    @Column(length = 64)
    private String kid;

    @Column(name = "public_key", nullable = false, columnDefinition = "text")
    private String publicKey;

    @Column(name = "private_key", nullable = false, columnDefinition = "text")
    private String privateKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SigningKey() {
    }

    public SigningKey(String kid, String publicKey, String privateKey) {
        this.kid = kid;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.createdAt = Instant.now();
    }

    public String getKid() {
        return kid;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }
}
