package org.cts.fp_identity.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.ForgotPasswordRequest;
import org.cts.fp_identity.dto.request.LoginRequest;
import org.cts.fp_identity.dto.request.ResetPasswordRequest;
import org.cts.fp_identity.dto.request.UpdatePasswordRequest;
import org.cts.fp_identity.dto.response.LoginResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.security.JwtUtil;
import org.cts.fp_identity.security.UserPrincipal;
import org.cts.fp_identity.service.AuthService;
import org.cts.fp_identity.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request, Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        authService.resetPassword(principal.getEmail(), request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String token = authService.generateResetToken(request.getEmail());
        emailService.sendResetEmail(request.getEmail(), token);
        return ResponseEntity.ok(ApiResponse.success("Reset link sent to email", null));
    }

    @PostMapping("/update-password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(@RequestBody UpdatePasswordRequest request) {
        authService.updatePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", null));
    }

    /**
     * Called by fp_gateway on every authenticated request.
     * Validates JWT signature + checks the blacklist so that
     * logged-out tokens are rejected across all microservices.
     * Endpoint is under /api/auth/** which is already permit-all in SecurityConfig.
     */
    @GetMapping("/token/validate")
    public ResponseEntity<ApiResponse<Void>> validateToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Missing or invalid Authorization header"));
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid or expired token"));
        }
        if (authService.isTokenBlacklisted(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token has been invalidated. Please login again."));
        }
        return ResponseEntity.ok(ApiResponse.success("Token is valid", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
