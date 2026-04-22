package org.cts.fp_reporting.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class ReportRequest {

    @NotBlank(message = "Scope is required")
    private String scope;

    private Map<String, Object> parametersJson;
}
