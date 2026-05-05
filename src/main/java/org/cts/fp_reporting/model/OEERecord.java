package org.cts.fp_reporting.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "oee_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OEERecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oeeId;

    private Long lineId;
    private String lineName;

    private Long shiftId;
    private String shiftName;

    private LocalDate date;

    private Double availabilityPct;
    private Double performancePct;
    private Double qualityPct;
    private Double oeePct;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
