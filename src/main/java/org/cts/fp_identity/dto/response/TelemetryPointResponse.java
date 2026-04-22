package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TelemetryPointResponse {
    private Long pointId;
    private Long machineId;
    private String machineName;
    private String name;
    private String dataType;
    private String unit;
}
