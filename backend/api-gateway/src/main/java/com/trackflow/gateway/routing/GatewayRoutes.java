package com.trackflow.gateway.routing;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Proxies business APIs to the services registered in Eureka ({@code lb()} resolves the name to an
 * instance). The caller's Authorization header travels with the request, so each service can verify
 * the token and apply its own ownership rules. {@code /api/auth/**} is served by the gateway itself.
 */
@Configuration
public class GatewayRoutes {

    @Bean
    RouterFunction<ServerResponse> orderServiceRoute() {
        return route("order-service")
                .route(path("/api/orders", "/api/orders/**"), http())
                .filter(lb("order-service"))
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> deliveryServiceRoute() {
        return route("delivery-service")
                .route(path("/api/deliveries", "/api/deliveries/**", "/api/couriers", "/api/couriers/**"), http())
                .filter(lb("delivery-service"))
                .build();
    }
}
