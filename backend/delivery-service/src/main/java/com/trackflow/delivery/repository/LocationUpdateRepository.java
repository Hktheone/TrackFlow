package com.trackflow.delivery.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trackflow.delivery.domain.LocationUpdate;

public interface LocationUpdateRepository extends JpaRepository<LocationUpdate, Long> {

    List<LocationUpdate> findByDeliveryIdOrderByRecordedAtAscIdAsc(UUID deliveryId);
}
