package com.trackflow.delivery.web.dto;

import java.time.Instant;

import com.trackflow.delivery.domain.LocationUpdate;

public record LocationPointResponse(double latitude, double longitude, Instant recordedAt) {

    public static LocationPointResponse from(LocationUpdate update) {
        return new LocationPointResponse(update.getLatitude(), update.getLongitude(), update.getRecordedAt());
    }
}
