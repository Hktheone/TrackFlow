package com.trackflow.delivery.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trackflow.delivery.service.CourierService;
import com.trackflow.delivery.web.dto.CourierResponse;
import com.trackflow.delivery.web.dto.CreateCourierRequest;

import jakarta.validation.Valid;

/** Riders (couriers): customers and admins can list them to pick one; only admins add new ones. */
@RestController
@RequestMapping("/api/couriers")
public class CourierController {

    private final CourierService courierService;

    public CourierController(CourierService courierService) {
        this.courierService = courierService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<CourierResponse> list() {
        return courierService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CourierResponse create(@Valid @RequestBody CreateCourierRequest request) {
        return courierService.create(request);
    }
}
