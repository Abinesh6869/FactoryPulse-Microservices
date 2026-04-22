package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.ResetPasswordRequest;
import org.cts.fp_identity.dto.request.UpdatePasswordRequest;
import org.cts.fp_identity.dto.response.LoginResponse;
import org.cts.fp_identity.dto.request.LoginRequest;
import org.cts.fp_identity.exception.BadRequestException;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.exception.UnauthorizedException;
import org.cts.fp_identity.model.BlacklistedToken;
import org.cts.fp_identity.model.User;
import org.cts.fp_identity.repository.BlacklistedTokenRepository;
import org.cts.fp_identity.repository.UserRepository;
import org.cts.fp_identity.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final AuditLogService auditLogService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new UnauthorizedException("Wrong password");
        if ("INACTIVE".equalsIgnoreCase(user.getStatus()))
            throw new BadRequestException("User account is inactive");
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getUserId(),
                user.getUserName(), user.getEmployeeId());
        auditLogService.log(user, "LOGIN", "User", "Logged in: " + user.getEmail());
        return new LoginResponse(token, user.getRole().name(), user.getUserId(), user.getUserName(), user.getEmployeeId());
    }

    public void resetPassword(String email, ResetPasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash()))
            throw new UnauthorizedException("Old password doesn't match");
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditLogService.log(user, "RESET_PASSWORD", "User", "Password reset for: " + email);
    }

    public String generateResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        auditLogService.log(user, "FORGOT_PASSWORD", "User", "Reset token generated for: " + email);
        return token;
    }

    public void updatePassword(UpdatePasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid reset token"));
        if (user.getTokenExpiry().isBefore(LocalDateTime.now()))
            throw new BadRequestException("Token has expired");
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);
        auditLogService.log(user, "UPDATE_PASSWORD", "User", "Password updated via token for: " + user.getEmail());
    }

    public void logout(String token) {
        if (blacklistedTokenRepository.existsByToken(token))
            throw new BadRequestException("Already logged out");
        BlacklistedToken blacklisted = new BlacklistedToken();
        blacklisted.setToken(token);
        blacklisted.setExpiresAt(jwtUtil.extractExpiry(token));
        blacklistedTokenRepository.save(blacklisted);
        String email = jwtUtil.extractEmail(token);
        auditLogService.log("LOGOUT", "User", "Logged out: " + email);
    }
}
