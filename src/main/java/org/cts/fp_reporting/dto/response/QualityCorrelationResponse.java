package org.cts.fp_reporting.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QualityCorrelationResponse {
    private Long qualityRecordId;
    private Long lineId;
    private String lineName;
    private Long productionCountId;
    private Integer goodCount;
    private Integer rejectCount;
    private LocalDateTime productionTimestamp;
    private Long downtimeEventId;
    private Long telemetryEventId;
    private Long reviewedById;
    private String employeeId;
    private String reviewedByName;
    private String notes;
    private LocalDateTime createdAt;
}
