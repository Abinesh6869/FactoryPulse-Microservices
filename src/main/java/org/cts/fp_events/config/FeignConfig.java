package org.cts.fp_events.config;

import feign.RequestInterceptor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.cts.fp_events.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    private final JwtUtil jwtUtil;
    private String serviceToken;

    @PostConstruct
    public void init() {
        this.serviceToken = jwtUtil.generateServiceToken();
    }

    @Bean
    public RequestInterceptor jwtRequestInterceptor() {
        return requestTemplate ->
                requestTemplate.header("Authorization", "Bearer " + serviceToken);
    }
}
