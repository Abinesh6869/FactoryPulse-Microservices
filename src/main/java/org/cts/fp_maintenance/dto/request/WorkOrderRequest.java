package org.cts.fp_maintenance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkOrderRequest {

    @NotNull(message = "Machine ID is required")
    private Long machineId;

    private String machineName;

    @NotNull(message = "Downtime ID is required to create a work order")
    private Long downtimeId;

    @NotBlank(message = "Priority is required")
    private String priority;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Assigned Technician ID is required")
    private Long assignedToUserId;

    private String assignedToName;

    private String assignedToEmployeeId;
}
