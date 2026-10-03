package com.trackflow.delivery.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.trackflow.delivery.domain.Courier;
import com.trackflow.delivery.domain.VehicleType;

public record CourierResponse(UUID id, String name, String username, String phone, VehicleType vehicleType, boolean available,
                              Double latitude, Double longitude, Instant lastLocationAt) {

    public static CourierResponse from(Courier c) {
        return new CourierResponse(c.getId(), c.getName(), c.getUsername(), c.getPhone(), c.getVehicleType(), c.isAvailable(),
                c.getCurrentLatitude(), c.getCurrentLongitude(), c.getLastLocationAt());
    }
}
