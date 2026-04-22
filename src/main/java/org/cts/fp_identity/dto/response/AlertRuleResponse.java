package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AlertRuleResponse {
    private Long ruleId;
    private String name;
    private String triggerExpression;
    private String severity;
    private List<String> recipientsJson;
    private Boolean active;
}
