package org.cts.fp_telemetry.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductionCountResponse {
    private Long countId;
    private Long lineId;
    private String lineName;
    private Long shiftId;
    private String shiftName;
    private LocalDateTime timestamp;
    private Integer goodCount;
    private Integer rejectCount;
    private Integer totalCount;
}
