package org.cts.fp_events.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CorrectiveActionRequest {

    @NotNull(message = "Downtime ID is required")
    private Long downtimeId;

    @NotNull(message = "Assigned user ID is required")
    private Long assignedTo;

    // Optional — auto-resolved from fp_identity if not provided
    private String assignedToEmployeeId;

    // Optional — auto-resolved from fp_identity if not provided
    private String assignedToName;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "DueDate is required")
    private LocalDate dueDate;
}
