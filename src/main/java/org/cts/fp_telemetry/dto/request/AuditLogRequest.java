package org.cts.fp_telemetry.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogRequest {
    private String action;
    private String resource;
    private String details;
}
