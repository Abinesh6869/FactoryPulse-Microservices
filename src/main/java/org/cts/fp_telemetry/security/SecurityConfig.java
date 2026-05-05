package org.cts.fp_telemetry.security;

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
                // Manual telemetry/production submissions — plant-floor roles only
                .requestMatchers(HttpMethod.POST,  "/api/telemetry/**").hasAnyRole("OPERATOR", "SUPERVISOR", "ADMIN")
                // Update production counts — ADMIN and OPERATOR only (matches monolith)
                .requestMatchers(HttpMethod.PATCH, "/api/telemetry/production/updateCount/**").hasAnyRole("ADMIN", "OPERATOR")
                // Other PATCH — SUPERVISOR can also update
                .requestMatchers(HttpMethod.PATCH, "/api/telemetry/**").hasAnyRole("OPERATOR", "SUPERVISOR", "ADMIN")
                // Read telemetry and production data — all plant-floor + management + quality roles
                .requestMatchers(HttpMethod.GET,   "/api/telemetry/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "OPERATOR", "TECHNICIAN", "QUALITY_ENGINEER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
