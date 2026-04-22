package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long auditId;
    private Long userId;
    private String userName;
    private String employeeId;
    private String action;
    private String resource;
    private LocalDateTime timestamp;
    private String details;
}
