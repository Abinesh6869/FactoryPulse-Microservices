package org.cts.fp_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.cts.fp_gateway.util.JwtUtil;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final WebClient webClient;

    /**
     * Paths that bypass JWT validation entirely.
     * /api/auth/** covers login, refresh, register — no token yet.
     * /swagger-ui and /v3/api-docs allow Swagger UIs on downstream services
     * to be reached without a token (Swagger itself sends the token per-request).
     */
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth/",
            "/swagger-ui",
            "/v3/api-docs"
    );

    public JwtAuthFilter(JwtUtil jwtUtil,
                         ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        this.jwtUtil = jwtUtil;
        // Uses service discovery — resolves "fp-identity" from Eureka via LoadBalancer
        this.webClient = WebClient.builder()
                .baseUrl("http://fp-identity")
                .filter(lbFunction)
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Always pass through CORS preflight — browser sends OPTIONS before every
        // cross-origin request and never includes a token on preflight.
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        // Step 1: validate JWT signature + expiry locally (fast, no network)
        try {
            jwtUtil.validateToken(authHeader.substring(7));
        } catch (Exception e) {
            log.warn("Gateway JWT validation failed for path {}: {}", path, e.getMessage());
            return sendUnauthorized(exchange, "Invalid or expired token");
        }

        // Step 2: check token blacklist via fp_identity (catches logged-out tokens)
        // Resolve blacklist check first, THEN call chain.filter so routing errors
        // are never caught by the blacklist onErrorResume (which caused empty 200 when
        // a downstream service was down).
        final String finalAuthHeader = authHeader;
        Mono<Boolean> blacklisted = webClient.get()
                .uri("/api/auth/token/validate")
                .header(HttpHeaders.AUTHORIZATION, finalAuthHeader)
                .retrieve()
                .toBodilessEntity()
                .map(response -> false)
                .onErrorResume(ex -> {
                    if (ex instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcex
                            && wcex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        log.warn("Blacklisted token rejected for path {}", path);
                        return Mono.just(true);
                    }
                    log.warn("Could not reach fp_identity for blacklist check on path {}: {} — allowing through", path, ex.getMessage());
                    return Mono.just(false);
                });

        return blacklisted.flatMap(isBlacklisted -> {
            if (isBlacklisted) {
                return sendUnauthorized(exchange, "Token has been invalidated. Please login again.");
            }
            return chain.filter(exchange);
        });
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> sendUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"success\":false,\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // Run before all other filters
    }
}
