package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;
import org.cts.fp_identity.model.Role;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long userId;
    private String employeeId;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
