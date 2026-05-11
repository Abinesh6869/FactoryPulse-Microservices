package org.cts.fp_telemetry.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.of;

@Order(2)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JwtUtilTest {

    private static final String SECRET = "test-secret-key-which-is-long-enough-32chars!";

    private static JwtUtil jwtUtil;
    private static String validToken;

    @BeforeAll
    static void init() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "SECRET_KEY", SECRET);
        jwtUtil.init();

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        validToken = Jwts.builder()
                .setSubject("operator@example.com")
                .claim("role", "OPERATOR")
                .claim("userId", 42)
                .claim("userName", "Operator")
                .claim("employeeId", "OP0001")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    @Test
    @Order(1)
    void validateToken_validToken_returnsTrue() {
        assertTrue(jwtUtil.validateToken(validToken));
    }

    @ParameterizedTest
    @MethodSource("data")
    @Order(2)
    void validateToken_invalidTokens_returnsFalse(String token) {
        assertFalse(jwtUtil.validateToken(token));
    }

    public static Stream<org.junit.jupiter.params.provider.Arguments> data() {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String expired = Jwts.builder()
                .setSubject("operator@example.com")
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();
        return Stream.of(
                of("not.a.token"),
                of("random-invalid-value"),
                of(expired)
        );
    }

    @Test
    @Order(3)
    void extractEmail_returnsCorrectEmail() {
        assertEquals("operator@example.com", jwtUtil.extractEmail(validToken));
    }

    @Test
    @Order(4)
    void extractRole_returnsCorrectRole() {
        assertEquals("OPERATOR", jwtUtil.extractRole(validToken));
    }

    @Test
    @Order(5)
    void extractUserId_returnsCorrectId() {
        assertEquals(42L, jwtUtil.extractUserId(validToken));
    }

    @Test
    @Order(6)
    void generateServiceToken_isValid() {
        assertTrue(jwtUtil.validateToken(jwtUtil.generateServiceToken()));
    }

    @Test
    @Order(7)
    void generateServiceToken_hasAdminRole() {
        assertEquals("ADMIN", jwtUtil.extractRole(jwtUtil.generateServiceToken()));
    }

    @Test
    @Order(8)
    void generateServiceToken_hasSystemEmail() {
        assertEquals("system@factorypulse.internal", jwtUtil.extractEmail(jwtUtil.generateServiceToken()));
    }
}