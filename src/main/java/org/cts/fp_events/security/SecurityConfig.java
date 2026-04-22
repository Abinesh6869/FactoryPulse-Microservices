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
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Downtime events: operators/supervisors/admins can create; technicians/supervisors/admins can close/tag
                .requestMatchers(HttpMethod.POST, "/api/downtimes/**").hasAnyRole("OPERATOR", "SUPERVISOR", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/downtimes/**").hasAnyRole("TECHNICIAN", "SUPERVISOR", "ADMIN")
                // Alerts: marking own notification as read is authenticated; resolving alert requires elevated role
                .requestMatchers(HttpMethod.PATCH, "/api/alerts/notifications/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/alerts/**").hasAnyRole("SUPERVISOR", "MANAGER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
