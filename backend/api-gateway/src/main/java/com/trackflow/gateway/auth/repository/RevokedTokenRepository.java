package com.trackflow.gateway.auth.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.trackflow.gateway.auth.domain.RevokedToken;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    @Modifying
    @Transactional
    @Query("delete from RevokedToken t where t.expiresAt < :now")
    int deleteExpired(Instant now);
}
