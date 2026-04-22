package org.cts.fp_identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AlertRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    @NotBlank(message = "Trigger expression is required")
    private String triggerExpression;

    @NotBlank(message = "Severity is required")
    private String severity;

    private List<String> recipientsJson;

    @NotNull(message = "Active status is required")
    private Boolean active = true;
}
