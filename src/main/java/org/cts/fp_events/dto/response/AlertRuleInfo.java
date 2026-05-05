package org.cts.fp_events.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertRuleInfo {
    private Long ruleId;
    private String name;
    private String triggerExpression;
    private String severity;
    private List<String> recipientsJson;   // already deserialized by fp_identity
    private Boolean active;
}
