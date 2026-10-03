package com.trackflow.gateway.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trackflow.gateway.auth.domain.SigningKey;

public interface SigningKeyRepository extends JpaRepository<SigningKey, String> {

    Optional<SigningKey> findFirstByOrderByCreatedAtDesc();
}
