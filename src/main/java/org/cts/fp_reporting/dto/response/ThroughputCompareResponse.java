package org.cts.fp_reporting.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ThroughputCompareResponse {
    private Long forecastId;
    private Long lineId;
    private String lineName;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private int expectedUnits;
    private Double confidence;
    private long actualUnits;
    private long goodUnits;
    private long rejectUnits;
    private double achievementPct;
    private String status;
}
