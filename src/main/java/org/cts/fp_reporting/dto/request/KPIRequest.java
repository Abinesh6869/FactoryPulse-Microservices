package org.cts.fp_reporting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KPIRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String definition;

    @NotNull(message = "Target value is required")
    private Double target;

    private Double currentValue;
    private String reportingPeriod;
}
