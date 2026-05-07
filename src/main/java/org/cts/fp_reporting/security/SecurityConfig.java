package org.cts.fp_reporting.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Reports
                .requestMatchers(HttpMethod.POST, "/api/reports/**").hasAnyRole("ADMIN", "MANAGER", "QUALITY_ENGINEER", "ANALYST")
                .requestMatchers(HttpMethod.GET,  "/api/reports/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "QUALITY_ENGINEER", "ANALYST")
                // OEE
                .requestMatchers(HttpMethod.POST,  "/api/oee", "/api/oee/**").hasAnyRole("ADMIN", "MANAGER", "ANALYST")
                .requestMatchers(HttpMethod.GET,   "/api/oee", "/api/oee/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "QUALITY_ENGINEER", "ANALYST", "OPERATOR")
                // KPI
                .requestMatchers(HttpMethod.POST,  "/api/kpis/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PUT,   "/api/kpis/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/api/kpis/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.GET,   "/api/kpis/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "QUALITY_ENGINEER", "ANALYST", "OPERATOR")
                // Throughput forecasts
                .requestMatchers(HttpMethod.POST,  "/api/throughput-forecasts", "/api/throughput-forecasts/**").hasAnyRole("ADMIN", "MANAGER", "ANALYST")
                .requestMatchers(HttpMethod.GET,   "/api/throughput-forecasts", "/api/throughput-forecasts/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "QUALITY_ENGINEER", "ANALYST", "OPERATOR")
                // Quality correlation
                .requestMatchers(HttpMethod.POST,  "/api/quality-correlation/**").hasAnyRole("ADMIN", "MANAGER", "QUALITY_ENGINEER", "ANALYST")
                .requestMatchers(HttpMethod.PATCH, "/api/quality-correlation/**").hasAnyRole("ADMIN", "MANAGER", "QUALITY_ENGINEER", "ANALYST")
                .requestMatchers(HttpMethod.GET,   "/api/quality-correlation/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "QUALITY_ENGINEER", "ANALYST", "OPERATOR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
