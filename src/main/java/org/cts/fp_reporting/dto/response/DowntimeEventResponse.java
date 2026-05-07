package org.cts.fp_reporting.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DowntimeEventResponse {
    private Long downtimeId;
    private Long lineId;
    private Long machineId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long durationSec;
}
