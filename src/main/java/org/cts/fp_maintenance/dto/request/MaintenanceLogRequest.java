package org.cts.fp_maintenance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MaintenanceLogRequest {

    @NotNull(message = "Work order ID is required")
    private Long workOrderId;

    private String notes;

    private List<Map<String, Object>> partsUsedJson;

    private Integer timeSpentMinutes;
}
