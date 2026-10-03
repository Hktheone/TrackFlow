package com.trackflow.gateway.auth.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trackflow.gateway.auth.security.SigningKeys;

/** Publishes the public signing key so other services can verify tokens without calling the gateway per request. */
@RestController
public class JwksController {

    private final SigningKeys signingKeys;

    public JwksController(SigningKeys signingKeys) {
        this.signingKeys = signingKeys;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return signingKeys.publicJwks();
    }
}
