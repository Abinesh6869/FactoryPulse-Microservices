package org.cts.fp_reporting.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ThroughputForecastResponse {
    private Long forecastId;
    private Long lineId;
    private String lineName;
    private String plantName;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private int expectedUnits;
    private Double confidence;
}
