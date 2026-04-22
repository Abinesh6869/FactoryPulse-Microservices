package org.cts.fp_events.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertResponse {
    private Long alertId;
    private Long ruleId;
    private String ruleName;
    private String severity;
    private String entityType;
    private Long relatedEntityId;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;
    private String status;
    private String notes;
}
