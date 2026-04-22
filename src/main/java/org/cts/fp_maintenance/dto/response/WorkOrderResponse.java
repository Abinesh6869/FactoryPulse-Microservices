package org.cts.fp_maintenance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkOrderResponse {
    private Long workOrderId;
    private Long machineId;
    private String machineName;
    private Long downtimeId;
    private Long createdBy;
    private String createdByEmployeeId;
    private String createdByName;
    private String priority;
    private String description;
    private Long assignedToId;
    private String assignedToEmployeeId;
    private String assignedToName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String warning;
}
