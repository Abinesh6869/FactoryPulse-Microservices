package org.cts.fp_telemetry.config;

import feign.RequestInterceptor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.cts.fp_telemetry.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    private final JwtUtil jwtUtil;
    private String serviceToken;

    @PostConstruct
    public void init() {
        // Generate once at startup — used by scheduler threads that have no HTTP context
        this.serviceToken = jwtUtil.generateServiceToken();
    }

    @Bean
    public RequestInterceptor jwtRequestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                // HTTP request context — forward the user's token
                String auth = attrs.getRequest().getHeader("Authorization");
                if (auth != null) {
                    requestTemplate.header("Authorization", auth);
                    return;
                }
            }
            // No HTTP context (scheduled task) — use service token
            requestTemplate.header("Authorization", "Bearer " + serviceToken);
        };
    }
}
