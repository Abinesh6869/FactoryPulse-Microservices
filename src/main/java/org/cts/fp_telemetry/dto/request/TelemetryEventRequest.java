package org.cts.fp_telemetry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TelemetryEventRequest {

    @NotNull(message = "pointId is required")
    private Long pointId;

    @NotBlank(message = "pointName is required")
    private String pointName;

    @NotNull(message = "machineId is required")
    private Long machineId;

    @NotBlank(message = "machineName is required")
    private String machineName;

    @NotBlank(message = "value is required")
    private String value;

    /** Source of the reading, e.g. "MANUAL", "SENSOR", "SIMULATOR" */
    private String source = "MANUAL";

    /** Status of the reading, e.g. "OK", "ALERT", "WARNING" */
    private String status = "OK";
}
