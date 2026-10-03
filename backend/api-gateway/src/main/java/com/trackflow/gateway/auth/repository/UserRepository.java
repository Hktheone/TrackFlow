package com.trackflow.gateway.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trackflow.gateway.auth.domain.AppUser;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    List<AppUser> findAllByOrderByUsernameAsc();
}
