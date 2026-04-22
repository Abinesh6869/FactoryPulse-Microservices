package org.cts.fp_telemetry.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductionCountRequest {

    @NotNull(message = "lineId is required")
    private Long lineId;

    @NotBlank(message = "lineName is required")
    private String lineName;

    private Long shiftId;

    private String shiftName;

    @NotNull(message = "goodCount is required")
    @Min(value = 0, message = "goodCount must be >= 0")
    private Integer goodCount;

    @NotNull(message = "rejectCount is required")
    @Min(value = 0, message = "rejectCount must be >= 0")
    private Integer rejectCount;
}
