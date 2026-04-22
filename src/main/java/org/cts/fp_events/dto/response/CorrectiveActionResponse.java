package org.cts.fp_events.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CorrectiveActionResponse {
    private Long actionId;
    private Long downtimeId;
    private String machineName;
    private String lineName;
    private String rootCauseCode;
    private Long assignedTo;
    private String assignedToEmployeeId;
    private String assignedToName;
    private String description;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private String status;
    private LocalDateTime createdAt;
}
