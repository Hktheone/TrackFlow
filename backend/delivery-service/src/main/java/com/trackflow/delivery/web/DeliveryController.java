package com.trackflow.delivery.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trackflow.delivery.domain.DeliveryStatus;
import com.trackflow.delivery.security.CurrentUser;
import com.trackflow.delivery.service.DeliveryService;
import com.trackflow.delivery.web.dto.AssignCourierRequest;
import com.trackflow.delivery.web.dto.DeliveryResponse;
import com.trackflow.delivery.web.dto.LocationPointResponse;
import com.trackflow.delivery.web.dto.LocationUpdateRequest;
import com.trackflow.delivery.web.dto.UpdateStatusRequest;

import jakarta.validation.Valid;

/**
 * Deliveries are created from Kafka events, never through this API, so there is no POST /api/deliveries.
 * Every role may call these endpoints; {@code DeliveryService} decides what each caller can see and do.
 */
@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public List<DeliveryResponse> list(@RequestParam(required = false) DeliveryStatus status,
                                       @AuthenticationPrincipal Jwt jwt) {
        return deliveryService.findAll(status, CurrentUser.from(jwt));
    }

    @GetMapping("/{id}")
    public DeliveryResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return deliveryService.findById(id, CurrentUser.from(jwt));
    }

    @GetMapping("/order/{orderId}")
    public DeliveryResponse getByOrder(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        return deliveryService.findByOrderId(orderId, CurrentUser.from(jwt));
    }

    @PostMapping("/{id}/assign")
    public DeliveryResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignCourierRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return deliveryService.assignCourier(id, request.courierId(), CurrentUser.from(jwt));
    }

    @PatchMapping("/{id}/status")
    public DeliveryResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        return deliveryService.updateStatus(id, request.status(), request.note(), CurrentUser.from(jwt));
    }

    @PostMapping("/{id}/location")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationPointResponse recordLocation(@PathVariable UUID id, @Valid @RequestBody LocationUpdateRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        return deliveryService.recordLocation(id, request.latitude(), request.longitude(), CurrentUser.from(jwt));
    }

    @GetMapping("/{id}/track")
    public List<LocationPointResponse> track(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return deliveryService.trail(id, CurrentUser.from(jwt));
    }
}
