package org.cts.fp_reporting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QualityCorrelationRequest {

    @NotNull(message = "Production count ID is required")
    private Long productionCountId;

    private Long downtimeEventId;
    private Long telemetryEventId;

    @NotBlank(message = "Notes are required")
    private String notes;
}
