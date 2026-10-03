package com.trackflow.delivery.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "couriers")
public class Courier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String phone;

    /** Username of the RIDER account that carries this courier profile. */
    @Column(unique = true, length = 50)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    /** False while the courier is carrying an active delivery. */
    @Column(nullable = false)
    private boolean available = true;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "last_location_at")
    private Instant lastLocationAt;

    @Column(name = "last_assigned_at")
    private Instant lastAssignedAt;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Courier() {
    }

    public Courier(String name, String phone, VehicleType vehicleType, String username) {
        this.name = name;
        this.phone = phone;
        this.username = username;
        this.vehicleType = vehicleType;
        this.createdAt = Instant.now();
    }

    void markAssigned() {
        this.available = false;
        this.lastAssignedAt = Instant.now();
    }

    void release() {
        this.available = true;
    }

    public void moveTo(double latitude, double longitude) {
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
        this.lastLocationAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getUsername() {
        return username;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public boolean isAvailable() {
        return available;
    }

    public Double getCurrentLatitude() {
        return currentLatitude;
    }

    public Double getCurrentLongitude() {
        return currentLongitude;
    }

    public Instant getLastLocationAt() {
        return lastLocationAt;
    }
}
