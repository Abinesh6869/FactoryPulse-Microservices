package org.cts.fp_reporting.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KPIResponse {
    private Long kpiId;
    private String name;
    private String definition;
    private Double target;
    private Double currentValue;
    private String reportingPeriod;
    private String status;
}
