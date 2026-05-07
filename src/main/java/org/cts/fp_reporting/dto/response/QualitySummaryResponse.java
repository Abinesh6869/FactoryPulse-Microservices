package org.cts.fp_reporting.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QualitySummaryResponse {
    private Long lineId;
    private String lineName;
    private List<QualityCorrelationResponse> qualityRecords;
    private List<ProductionCountResponse> productionCounts;
}
