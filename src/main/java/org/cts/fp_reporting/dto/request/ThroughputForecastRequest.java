package org.cts.fp_reporting.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThroughputForecastRequest {

    @NotNull(message = "Line Id is required")
    private Long lineId;

    // auto-populated from identity service if not provided
    private String lineName;
    private String plantName;

    @NotNull(message = "Period start is required")
    private LocalDateTime periodStart;

    @NotNull(message = "Period end is required")
    private LocalDateTime periodEnd;

    @NotNull(message = "Expected Unit is required")
    @Min(value = 1, message = "The value should be greater than or equal to 1")
    private int expectedUnits;

    @NotNull(message = "Confidence is required")
    @DecimalMin(value = "0.0", message = "Confidence should be greater than 0")
    @DecimalMax(value = "100.0", message = "Confidence should be lesser than 100")
    private Double confidence;
}
