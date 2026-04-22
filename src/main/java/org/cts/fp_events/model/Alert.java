package org.cts.fp_events.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
