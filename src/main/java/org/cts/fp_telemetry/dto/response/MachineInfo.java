package org.cts.fp_telemetry.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MachineInfo {
    private Long machineId;
    private String name;
    private String status;
    private Long lineId;
    private String lineName;
}
