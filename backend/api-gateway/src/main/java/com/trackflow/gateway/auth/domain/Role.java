package com.trackflow.gateway.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoleName name;

    @Column(nullable = false, length = 200)
    private String description;

    protected Role() {
    }

    public RoleName getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
