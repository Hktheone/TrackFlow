package com.trackflow.delivery.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trackflow.delivery.domain.Courier;
import com.trackflow.delivery.repository.CourierRepository;
import com.trackflow.delivery.web.dto.CourierResponse;
import com.trackflow.delivery.web.dto.CreateCourierRequest;

@Service
public class CourierService {

    private final CourierRepository couriers;

    public CourierService(CourierRepository couriers) {
        this.couriers = couriers;
    }

    @Transactional(readOnly = true)
    public List<CourierResponse> findAll() {
        return couriers.findAllByOrderByNameAsc().stream().map(CourierResponse::from).toList();
    }

    @Transactional
    public CourierResponse create(CreateCourierRequest request) {
        String username = request.username().trim().toLowerCase();
        if (couriers.existsByUsername(username)) {
            throw new DuplicateRiderException(username);
        }
        Courier courier = couriers.save(new Courier(request.name(), request.phone(), request.vehicleType(), username));
        return CourierResponse.from(courier);
    }
}
