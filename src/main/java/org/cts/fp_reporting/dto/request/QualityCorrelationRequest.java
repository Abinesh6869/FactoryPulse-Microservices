package org.cts.fp_reporting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QualityCorrelationRequest {

    @NotNull(message = "Line ID is required")
    private Long lineId;

    @NotBlank(message = "Line name is required")
    private String lineName;

    @NotNull(message = "Production count ID is required")
    private Long productionCountId;

    @NotNull(message = "Good count is required")
    private Integer goodCount;

    @NotNull(message = "Reject count is required")
    private Integer rejectCount;

    @NotNull(message = "Production timestamp is required")
    private LocalDateTime productionTimestamp;

    private Long downtimeEventId;
    private Long telemetryEventId;

    @NotBlank(message = "Notes are required")
    private String notes;
}
