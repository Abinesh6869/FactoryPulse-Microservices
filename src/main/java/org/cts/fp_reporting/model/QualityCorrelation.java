package org.cts.fp_reporting.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "quality_correlation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QualityCorrelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long qualityRecordId;

    private Long lineId;
    private String lineName;

    private Long productionCountId;
    private Integer goodCount;
    private Integer rejectCount;
    private LocalDateTime productionTimestamp;

    private Long downtimeEventId;
    private Long telemetryEventId;

    private Long reviewedById;
    private String reviewedByEmployeeId;
    private String reviewedByName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
