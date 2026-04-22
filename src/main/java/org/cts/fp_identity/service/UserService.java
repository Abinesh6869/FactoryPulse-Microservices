package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.cts.fp_identity.dto.request.UserRequest;
import org.cts.fp_identity.dto.request.UserUpdateRequest;
import org.cts.fp_identity.dto.response.BulkUserResult;
import org.cts.fp_identity.dto.response.UserResponse;
import org.cts.fp_identity.exception.BadRequestException;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.Role;
import org.cts.fp_identity.model.User;
import org.cts.fp_identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new BadRequestException("Email already in use: " + request.getEmail());
        User user = new User();
        user.setUserName(request.getName());
        user.setEmployeeId(generateEmployeeId(request.getRole()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);
        auditLogService.log("CREATE_USER", "User", "Created user: " + saved.getUserName() + ", role: " + saved.getRole());
        return toResponse(saved);
    }

    public List<UserResponse> getAllUsers(String search) {
        if (search == null || search.isBlank())
            return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
        return userRepository.findByUserNameContainingIgnoreCaseOrEmployeeIdContainingIgnoreCase(search, search)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<UserResponse> getUsersByRole(String role) {
        return userRepository.findByRole(Role.valueOf(role.toUpperCase()))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id)));
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        userRepository.findByEmail(request.getEmail())
                .filter(e -> !e.getUserId().equals(id))
                .ifPresent(e -> { throw new BadRequestException("Email already in use: " + request.getEmail()); });
        user.setUserName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        if (request.getRole() != null) user.setRole(request.getRole());
        User saved = userRepository.save(user);
        auditLogService.log("UPDATE_USER", "User", "Updated user ID: " + id);
        return toResponse(saved);
    }

    public UserResponse toggleUserStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setStatus(active ? "ACTIVE" : "INACTIVE");
        User saved = userRepository.save(user);
        auditLogService.log(active ? "ACTIVATE_USER" : "DEACTIVATE_USER", "User",
                (active ? "Activated" : "Deactivated") + " user ID: " + id);
        return toResponse(saved);
    }

    private static final Pattern NAME_PATTERN     = Pattern.compile("^[A-Za-z ]{3,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*[@#$%^&+=]).{6,}$");
    private static final Pattern PHONE_PATTERN    = Pattern.compile("^[0-9]{10}$");

    public BulkUserResult bulkCreateUsers(MultipartFile file) {
        List<BulkUserResult.SkippedRow> skipped = new ArrayList<>();
        List<BulkUserResult.FailedRow>  failed  = new ArrayList<>();
        List<UserResponse>              created = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csv = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true).setTrim(true)
                     .build().parse(reader)) {

            int rowNum = 1;
            for (CSVRecord record : csv) {
                rowNum++;
                String email = get(record, "email");
                try {
                    String name     = get(record, "name");
                    String phone    = get(record, "phone");
                    String roleStr  = get(record, "role").toUpperCase();
                    String password = get(record, "password");

                    if (!NAME_PATTERN.matcher(name).matches()) {
                        failed.add(new BulkUserResult.FailedRow(rowNum, email, "Name must be 3-20 letters/spaces")); continue;
                    }
                    if (!PHONE_PATTERN.matcher(phone).matches()) {
                        failed.add(new BulkUserResult.FailedRow(rowNum, email, "Phone must be 10 digits")); continue;
                    }
                    if (!PASSWORD_PATTERN.matcher(password).matches()) {
                        failed.add(new BulkUserResult.FailedRow(rowNum, email, "Password needs uppercase + special char, min 6")); continue;
                    }
                    Role role;
                    try { role = Role.valueOf(roleStr); }
                    catch (IllegalArgumentException ex) {
                        failed.add(new BulkUserResult.FailedRow(rowNum, email, "Invalid role: " + roleStr)); continue;
                    }
                    if (userRepository.existsByEmail(email)) {
                        skipped.add(new BulkUserResult.SkippedRow(rowNum, email, "Email already exists")); continue;
                    }
                    UserRequest req = new UserRequest();
                    req.setName(name); req.setEmail(email); req.setPhone(phone);
                    req.setRole(role); req.setPassword(password); req.setStatus("ACTIVE");
                    created.add(createUser(req));
                } catch (Exception ex) {
                    failed.add(new BulkUserResult.FailedRow(rowNum, email, ex.getMessage()));
                }
            }
        } catch (Exception ex) {
            throw new BadRequestException("Failed to parse CSV: " + ex.getMessage());
        }

        return BulkUserResult.builder()
                .totalRows(created.size() + skipped.size() + failed.size())
                .created(created.size()).skipped(skipped.size()).failed(failed.size())
                .createdUsers(created).skippedRows(skipped).failedRows(failed)
                .build();
    }

    private String get(CSVRecord r, String col) {
        try { return r.get(col) == null ? "" : r.get(col).trim(); }
        catch (Exception e) { return ""; }
    }

    private String generateEmployeeId(Role role) {
        String prefix = switch (role) {
            case ADMIN           -> "AD";
            case OPERATOR        -> "OP";
            case TECHNICIAN      -> "TC";
            case SUPERVISOR      -> "SV";
            case MANAGER         -> "MG";
            case QUALITY_ENGINEER -> "QE";
            case ANALYST         -> "AN";
        };
        Optional<String> lastId = userRepository.findEmployeeIdByRole(role);
        int nextNum = lastId.map(id -> Integer.parseInt(id.substring(2)) + 1).orElse(1);
        return prefix + String.format("%04d", nextNum);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .employeeId(user.getEmployeeId())
                .name(user.getUserName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
