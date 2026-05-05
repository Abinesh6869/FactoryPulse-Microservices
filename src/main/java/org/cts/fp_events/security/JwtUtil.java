package org.cts.fp_events.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;
    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String extractRole(String token) {
        return (String) Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("role");
    }

    public Long extractUserId(String token) {
        Object id = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("userId");
        return id != null ? Long.valueOf(id.toString()) : null;
    }

    public String extractUserName(String token) {
        return (String) Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("userName");
    }

    public String extractEmployeeId(String token) {
        return (String) Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("employeeId");
    }

    public String generateServiceToken() {
        long hundredYearsMs = 100L * 365 * 24 * 60 * 60 * 1000;
        return Jwts.builder()
                .setSubject("system@factorypulse.internal")
                .claim("role", "ADMIN")
                .claim("userId", 0L)
                .claim("userName", "System")
                .claim("employeeId", "SYS-001")
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + hundredYearsMs))
                .signWith(key)
                .compact();
    }
}
