package org.cts.fp_identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TelemetryPointRequest {
    @NotNull(message = "Machine ID is required")
    private Long machineId;

    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Data type is required")
    private String dataType;
    private String unit;
}
