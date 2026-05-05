package org.cts.fp_events.security;

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
                // Downtime: OPERATOR, SUPERVISOR, ADMIN can create
                .requestMatchers(HttpMethod.POST, "/api/downtimes/**").hasAnyRole("OPERATOR", "SUPERVISOR", "ADMIN")
                // Corrective action complete: TECHNICIAN, SUPERVISOR, ADMIN (specific rule BEFORE general PATCH)
                .requestMatchers(HttpMethod.PATCH, "/api/downtimes/actions/*/complete").hasAnyRole("ADMIN", "SUPERVISOR", "TECHNICIAN")
                // Close downtime / tag root cause: TECHNICIAN included so auto-close from fp_maintenance works
                .requestMatchers(HttpMethod.PATCH, "/api/downtimes/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR", "TECHNICIAN")
                // Downtime reads: all operational roles
                .requestMatchers(HttpMethod.GET, "/api/downtimes/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "OPERATOR", "TECHNICIAN", "QUALITY_ENGINEER", "ANALYST")
                // Internal notification creation — service-to-service only (ADMIN service token)
                .requestMatchers(HttpMethod.POST, "/api/alerts/notifications/internal").hasRole("ADMIN")
                // Alerts: resolve requires SUPERVISOR+
                .requestMatchers(HttpMethod.PATCH, "/api/alerts/*/resolve").hasAnyRole("ADMIN", "SUPERVISOR")
                // Notification read: OPERATOR, TECHNICIAN, SUPERVISOR, ADMIN
                .requestMatchers(HttpMethod.PATCH, "/api/alerts/notifications/**").hasAnyRole("ADMIN", "SUPERVISOR", "TECHNICIAN", "OPERATOR")
                // Alert reads: all operational roles
                .requestMatchers(HttpMethod.GET, "/api/alerts/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "OPERATOR", "TECHNICIAN", "QUALITY_ENGINEER", "ANALYST")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
