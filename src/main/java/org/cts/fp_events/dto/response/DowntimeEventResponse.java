package org.cts.fp_events.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DowntimeEventResponse {
    private Long downtimeId;
    private Long lineId;
    private String lineName;
    private Long machineId;
    private String machineName;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long durationSec;
    private String category;
    private Long rootCauseId;
    private String rootCauseCode;
    private String rootCauseDescription;
    private Long loggedBy;
    private String loggedByEmployeeId;
    private String loggedByName;
    private String notes;
    private LocalDateTime createdAt;
    private String warning;
}
