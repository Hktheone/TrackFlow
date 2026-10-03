package com.trackflow.gateway.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trackflow.gateway.auth.domain.Role;
import com.trackflow.gateway.auth.domain.RoleName;

public interface RoleRepository extends JpaRepository<Role, RoleName> {
}
