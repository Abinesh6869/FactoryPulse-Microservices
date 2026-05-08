package org.cts.fp_maintenance.security;

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
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                .requestMatchers(HttpMethod.POST,   "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers(HttpMethod.PUT,    "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR", "TECHNICIAN")
                .requestMatchers(HttpMethod.PATCH,  "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR", "TECHNICIAN")
                .requestMatchers(HttpMethod.DELETE, "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers(HttpMethod.GET,    "/api/workorders/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "TECHNICIAN")
                .requestMatchers(HttpMethod.POST,   "/api/maintenance-logs/**").hasAnyRole("ADMIN", "TECHNICIAN")
                .requestMatchers(HttpMethod.DELETE, "/api/maintenance-logs/**").hasAnyRole("ADMIN", "TECHNICIAN")
                .requestMatchers(HttpMethod.GET,    "/api/maintenance-logs/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "TECHNICIAN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
