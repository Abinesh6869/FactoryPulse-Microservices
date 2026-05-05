package org.cts.fp_maintenance.config;

import feign.RequestInterceptor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.cts.fp_maintenance.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    private final JwtUtil jwtUtil;
    private String serviceToken;

    @PostConstruct
    public void init() {
        // Generated once at startup — used when no HTTP request context is present
        this.serviceToken = jwtUtil.generateServiceToken();
    }

    @Bean
    public RequestInterceptor jwtRequestInterceptor() {
        // Always use the ADMIN service token for all service-to-service Feign calls.
        // Forwarding the user's token caused 403s when a non-ADMIN user (e.g. SUPERVISOR)
        // triggered a call to an endpoint that requires ADMIN (e.g. notifications/internal).
        return requestTemplate ->
                requestTemplate.header("Authorization", "Bearer " + serviceToken);
    }
}
