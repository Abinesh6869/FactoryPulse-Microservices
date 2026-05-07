package org.cts.fp_identity.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // USERS
                .requestMatchers(HttpMethod.POST,   "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/users/**").hasAnyRole("ADMIN", "SUPERVISOR")

                // PLANTS
                .requestMatchers(HttpMethod.POST,   "/api/plants/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/plants/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/plants/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/plants/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR")

                // LINES
                .requestMatchers(HttpMethod.POST,   "/api/lines/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/lines/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/lines/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/lines/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "OPERATOR", "TECHNICIAN", "QUALITY_ENGINEER", "ANALYST")

                // MACHINES
                .requestMatchers(HttpMethod.POST,   "/api/machines/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/machines/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/machines/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/machines/*/status").authenticated()
                .requestMatchers(HttpMethod.GET,    "/api/machines/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "OPERATOR", "TECHNICIAN", "QUALITY_ENGINEER")

                // SHIFTS
                .requestMatchers(HttpMethod.POST,   "/api/shifts", "/api/shifts/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/shifts", "/api/shifts/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/shifts", "/api/shifts/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/shifts", "/api/shifts/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "OPERATOR", "QUALITY_ENGINEER", "ANALYST")

                // TELEMETRY POINTS
                .requestMatchers(HttpMethod.POST,   "/api/telemetry-points/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/telemetry-points/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/telemetry-points/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/telemetry-points/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR", "TECHNICIAN", "QUALITY_ENGINEER")

                // ROOT CAUSES
                .requestMatchers(HttpMethod.POST,   "/api/rootcauses/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/rootcauses/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/rootcauses/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/rootcauses/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR", "TECHNICIAN", "QUALITY_ENGINEER", "MANAGER")

                // ALERT RULES
                .requestMatchers("/api/alertrules/**").hasRole("ADMIN")

                // MACHINE DOCUMENTS
                .requestMatchers(HttpMethod.POST,   "/api/machine-docs/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR")
                .requestMatchers(HttpMethod.PATCH,  "/api/machine-docs/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR")
                .requestMatchers(HttpMethod.DELETE, "/api/machine-docs/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR")
                .requestMatchers(HttpMethod.GET,    "/api/machine-docs/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR")

                // KPIs
                .requestMatchers(HttpMethod.POST,  "/api/kpis/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PUT,   "/api/kpis/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/api/kpis/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.GET,   "/api/kpis/**").hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "QUALITY_ENGINEER")

                // SHIFT ALLOCATIONS
                .requestMatchers(HttpMethod.POST,   "/api/shift-allocations/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers(HttpMethod.DELETE, "/api/shift-allocations/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers(HttpMethod.GET,    "/api/shift-allocations/**").hasAnyRole("ADMIN", "SUPERVISOR", "MANAGER", "OPERATOR", "TECHNICIAN")

                // AUDIT LOGS
                .requestMatchers(HttpMethod.POST, "/api/audit-logs/record").authenticated()
                .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")

                // NOTIFICATIONS
                .requestMatchers(HttpMethod.POST, "/api/notifications/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/notifications/**").authenticated()
                .requestMatchers(HttpMethod.PATCH,"/api/notifications/**").authenticated()

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
