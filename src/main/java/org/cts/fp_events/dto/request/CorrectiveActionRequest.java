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

    @NotBlank(message = "Assigned user employee ID is required")
    private String assignedToEmployeeId;

    @NotBlank(message = "Assigned user name is required")
    private String assignedToName;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "DueDate is required")
    private LocalDate dueDate;
}
