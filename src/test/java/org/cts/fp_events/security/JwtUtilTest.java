package org.cts.fp_events.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "super-secret-key-for-unit-tests-min32chars!!";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "SECRET_KEY", SECRET);
        // Manually trigger @PostConstruct
        jwtUtil.init();
    }

    // ── validateToken ─────────────────────────────────────────────────────────

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = buildToken("user@test.com", "ADMIN", 1L, "Test User", "EMP001",
                System.currentTimeMillis() + 3_600_000);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        String token = buildToken("user@test.com", "ADMIN", 1L, "Test User", "EMP001",
                System.currentTimeMillis() - 3_600_000);
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        assertThat(jwtUtil.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_emptyToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    void validateToken_wrongSecretToken_returnsFalse() {
        Key wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-minimum-32-characters!!".getBytes());
        String token = Jwts.builder()
                .setSubject("user@test.com")
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(wrongKey)
                .compact();
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    // ── extractEmail ──────────────────────────────────────────────────────────

    @Test
    void extractEmail_returnsSubject() {
        String token = buildToken("user@fp.com", "OPERATOR", 2L, "Jane", "EMP002",
                System.currentTimeMillis() + 3_600_000);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@fp.com");
    }

    // ── extractRole ───────────────────────────────────────────────────────────

    @Test
    void extractRole_returnsRoleClaim() {
        String token = buildToken("user@fp.com", "SUPERVISOR", 3L, "Bob", "EMP003",
                System.currentTimeMillis() + 3_600_000);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SUPERVISOR");
    }

    // ── extractUserId ─────────────────────────────────────────────────────────

    @Test
    void extractUserId_returnsUserIdClaim() {
        String token = buildToken("user@fp.com", "ADMIN", 42L, "Alice", "EMP042",
                System.currentTimeMillis() + 3_600_000);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42L);
    }

    // ── extractUserName ───────────────────────────────────────────────────────

    @Test
    void extractUserName_returnsUserNameClaim() {
        String token = buildToken("user@fp.com", "ADMIN", 1L, "John Doe", "EMP001",
                System.currentTimeMillis() + 3_600_000);
        assertThat(jwtUtil.extractUserName(token)).isEqualTo("John Doe");
    }

    // ── extractEmployeeId ─────────────────────────────────────────────────────

    @Test
    void extractEmployeeId_returnsEmployeeIdClaim() {
        String token = buildToken("user@fp.com", "ADMIN", 1L, "John", "EMP-XYZ",
                System.currentTimeMillis() + 3_600_000);
        assertThat(jwtUtil.extractEmployeeId(token)).isEqualTo("EMP-XYZ");
    }

    // ── generateServiceToken ──────────────────────────────────────────────────

    @Test
    void generateServiceToken_isValidAndContainsSystemSubject() {
        String token = jwtUtil.generateServiceToken();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("system@factorypulse.internal");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String buildToken(String email, String role, Long userId,
                              String userName, String employeeId, long expiryMs) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("userId", userId)
                .claim("userName", userName)
                .claim("employeeId", employeeId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(expiryMs))
                .signWith(KEY)
                .compact();
    }
}
