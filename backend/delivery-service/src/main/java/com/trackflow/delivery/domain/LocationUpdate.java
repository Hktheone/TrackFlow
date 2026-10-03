package com.trackflow.delivery.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One GPS ping from the courier while carrying a delivery; together they form the tracking trail. */
@Entity
@Table(name = "delivery_location_updates")
public class LocationUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Column(name = "courier_id")
    private UUID courierId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected LocationUpdate() {
    }

    public LocationUpdate(UUID deliveryId, UUID courierId, double latitude, double longitude) {
        this.deliveryId = deliveryId;
        this.courierId = courierId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.recordedAt = Instant.now();
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
