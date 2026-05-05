package org.cts.fp_reporting.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Kpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long kpiId;

    @Column(unique = true)
    private String name;

    private String definition;
    private Double target;
    private Double currentValue;
    private String reportingPeriod;
}
