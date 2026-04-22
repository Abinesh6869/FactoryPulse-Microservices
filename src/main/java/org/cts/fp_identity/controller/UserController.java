package org.cts.fp_identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.UserRequest;
import org.cts.fp_identity.dto.request.UserUpdateRequest;
import org.cts.fp_identity.dto.response.BulkUserResult;
import org.cts.fp_identity.dto.response.UserResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", userService.createUser(request)));
    }

    // GET /api/users
    // GET /api/users?id=1
    // GET /api/users?role=OPERATOR
    // GET /api/users?search=john
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getUsers(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String role,
            @RequestParam(required = false, defaultValue = "") String search) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("User fetched successfully", userService.getUserById(id)));
        if (role != null)
            return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", userService.getUsersByRole(role)));
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", userService.getAllUsers(search)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userService.updateUser(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleStatus(@PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                active ? "User activated" : "User deactivated", userService.toggleUserStatus(id, active)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkUserResult>> bulkCreate(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty())
            return ResponseEntity.badRequest().body(ApiResponse.error("CSV file is empty"));
        BulkUserResult result = userService.bulkCreateUsers(file);
        return ResponseEntity.ok(ApiResponse.success(
                "Bulk upload: " + result.getCreated() + " created, " + result.getSkipped() + " skipped, " + result.getFailed() + " failed",
                result));
    }
}
