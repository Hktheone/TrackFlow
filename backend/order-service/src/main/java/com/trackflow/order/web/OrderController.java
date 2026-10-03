package com.trackflow.order.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trackflow.order.domain.OrderStatus;
import com.trackflow.order.security.CurrentUser;
import com.trackflow.order.service.OrderService;
import com.trackflow.order.web.dto.CancelOrderRequest;
import com.trackflow.order.web.dto.CreateOrderRequest;
import com.trackflow.order.web.dto.OrderResponse;
import com.trackflow.order.web.dto.StatusHistoryResponse;

import jakarta.validation.Valid;

/** Customers (USER) work with their own orders; ADMIN with all of them. Riders have no access. */
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        OrderResponse created = orderService.create(request, CurrentUser.from(jwt));
        return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) OrderStatus status,
                                    @AuthenticationPrincipal Jwt jwt) {
        return orderService.findAll(status, CurrentUser.from(jwt));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.findById(id, CurrentUser.from(jwt));
    }

    @GetMapping("/{id}/history")
    public List<StatusHistoryResponse> history(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.history(id, CurrentUser.from(jwt));
    }

    @PostMapping("/{id}/confirm")
    public OrderResponse confirm(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.confirm(id, CurrentUser.from(jwt));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id, @Valid @RequestBody(required = false) CancelOrderRequest request,
                                @AuthenticationPrincipal Jwt jwt) {
        return orderService.cancel(id, request == null ? null : request.reason(), CurrentUser.from(jwt));
    }
}
