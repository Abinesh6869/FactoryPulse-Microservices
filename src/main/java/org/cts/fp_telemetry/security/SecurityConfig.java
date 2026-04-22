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
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Manual telemetry/production submissions — plant-floor roles only
                .requestMatchers(HttpMethod.POST,  "/api/telemetry/**").hasAnyRole("OPERATOR", "SUPERVISOR", "MANAGER", "ADMIN")
                // Update production counts — plant-floor roles only
                .requestMatchers(HttpMethod.PATCH, "/api/telemetry/**").hasAnyRole("OPERATOR", "SUPERVISOR", "MANAGER", "ADMIN")
                // Read telemetry and production data — all authenticated roles
                .requestMatchers(HttpMethod.GET,   "/api/telemetry/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "QUALITY_ENGINEER", "ANALYST", "OPERATOR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
