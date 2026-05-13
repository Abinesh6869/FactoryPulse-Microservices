package org.cts.fp_telemetry.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TelemetryEventResponse {
    private Long eventId;
    private Long pointId;
    private String pointName;
    private Long machineId;
    private String machineName;
    private Long lineId;
    private String lineName;
    private LocalDateTime timestamp;
    private String value;
    private String unit;
    private String source;
    private String status;
}
