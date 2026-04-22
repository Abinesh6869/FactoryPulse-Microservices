package org.cts.fp_reporting.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ReportResponse {
    private Long reportId;
    private String scope;
    private Map<String, Object> parametersJson;
    private Map<String, Object> metricsJson;
    private Long generatedBy;
    private String generatedByName;
    private LocalDateTime generatedAt;
    private String reportUrl;
}
