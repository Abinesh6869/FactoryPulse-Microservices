package org.cts.fp_identity.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ruleId;

    private String name;
    private String triggerExpression;
    private String severity;

    @Column(name = "recipients_json", columnDefinition = "TEXT")
    private String recipientsJson;

    private Boolean active;
}
