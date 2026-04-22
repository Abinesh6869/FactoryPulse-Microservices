package org.cts.fp_reporting.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    private String scope;

    @Column(name = "parameters_json", columnDefinition = "TEXT")
    private String parametersJson;

    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;

    // Denormalized — no JPA join to User (different service)
    private Long generatedById;
    private String generatedByName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime generatedAt;

    private String reportUrl;
}
