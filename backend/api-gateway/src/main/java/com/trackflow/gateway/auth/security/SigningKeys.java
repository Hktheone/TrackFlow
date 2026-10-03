package com.trackflow.gateway.auth.security;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.trackflow.gateway.auth.domain.SigningKey;
import com.trackflow.gateway.auth.repository.SigningKeyRepository;

/**
 * The RSA key that signs access tokens. Created once and stored in the auth database, so tokens stay
 * valid across restarts. Only the public half is ever published (via the JWKS endpoint), which is
 * what order-service and delivery-service use to verify tokens on their own.
 */
@Component
public class SigningKeys {

    private static final Logger log = LoggerFactory.getLogger(SigningKeys.class);

    private final RSAKey rsaKey;

    public SigningKeys(SigningKeyRepository repository) {
        this.rsaKey = repository.findFirstByOrderByCreatedAtDesc()
                .map(SigningKeys::load)
                .orElseGet(() -> create(repository));
    }

    public RSAKey rsaKey() {
        return rsaKey;
    }

    public RSAPublicKey publicKey() {
        try {
            return rsaKey.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalStateException("Signing key has no usable public key", e);
        }
    }

    /** The public key set served at {@code /.well-known/jwks.json}. */
    public Map<String, Object> publicJwks() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    private static RSAKey create(SigningKeyRepository repository) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            String kid = UUID.randomUUID().toString();
            Base64.Encoder base64 = Base64.getEncoder();
            repository.save(new SigningKey(kid,
                    base64.encodeToString(pair.getPublic().getEncoded()),
                    base64.encodeToString(pair.getPrivate().getEncoded())));
            log.info("Generated new token signing key {}", kid);
            return toRsaKey(kid, (RSAPublicKey) pair.getPublic(), (RSAPrivateKey) pair.getPrivate());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA is not available", e);
        }
    }

    private static RSAKey load(SigningKey stored) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            Base64.Decoder base64 = Base64.getDecoder();
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(base64.decode(stored.getPublicKey())));
            RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(base64.decode(stored.getPrivateKey())));
            return toRsaKey(stored.getKid(), publicKey, privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Stored signing key " + stored.getKid() + " is unreadable", e);
        }
    }

    private static RSAKey toRsaKey(String kid, RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(kid).build();
    }
}
