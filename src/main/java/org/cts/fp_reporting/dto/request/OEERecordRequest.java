package org.cts.fp_reporting.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OEERecordRequest {
    private String lineName;
    private String shiftName;
    private LocalDate shiftDate;
    private Long plannedSec;
    private Long totalDowntimeSec;
    private Long totalGoodCount;
    private Long totalRejectCount;
    private Long productionTickSec;
    private Long goodCountPerTick;
}
