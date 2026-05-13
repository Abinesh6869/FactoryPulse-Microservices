package org.cts.fp_telemetry.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetryPointInfo {
    private Long pointId;
    private String name;
    private Long machineId;
    private String unit;
}
