package org.cts.fp_events.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DowntimeEventRequest {

    @NotNull(message = "Line ID is required")
    private Long lineId;

    @NotBlank(message = "Line name is required")
    private String lineName;

    @NotNull(message = "Machine ID is required")
    private Long machineId;

    @NotBlank(message = "Machine name is required")
    private String machineName;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String category;

    private Long rootCauseId;
    private String rootCauseCode;
    private String rootCauseDescription;

    private String notes;
}
