package org.cts.fp_maintenance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MaintenanceLogResponse {
    private Long logId;
    private Long workOrderId;
    private Long machineId;
    private String machineName;
    private Long performedBy;
    private String performedByEmployeeId;
    private String performedByName;
    private LocalDateTime performedAt;
    private String notes;
    private List<PartUsed> partsUsedJson;
    private Integer timeSpentMinutes;
    private LocalDateTime createdAt;

    @Data
    public static class PartUsed {
        private String part;
        private String qty;
    }
}
