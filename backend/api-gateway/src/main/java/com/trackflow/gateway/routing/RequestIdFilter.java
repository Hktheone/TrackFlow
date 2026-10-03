package com.trackflow.gateway.routing;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tags every request with an X-Request-Id (reusing the caller's if present) and logs one line per
 * request, so a single call can be followed through the gateway and into the downstream service logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        String requestId = incoming != null ? incoming : UUID.randomUUID().toString();
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(incoming != null ? request : withHeader(request, requestId), response);
        } finally {
            if (request.getRequestURI().startsWith("/api/")) {
                log.info("{} {} -> {} ({} ms) [{}]", request.getMethod(), request.getRequestURI(),
                        response.getStatus(), System.currentTimeMillis() - start, requestId);
            }
        }
    }

    private static HttpServletRequest withHeader(HttpServletRequest request, String requestId) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return REQUEST_ID_HEADER.equalsIgnoreCase(name) ? requestId : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return REQUEST_ID_HEADER.equalsIgnoreCase(name)
                        ? Collections.enumeration(List.of(requestId))
                        : super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                names.add(REQUEST_ID_HEADER);
                return Collections.enumeration(names);
            }
        };
    }
}
