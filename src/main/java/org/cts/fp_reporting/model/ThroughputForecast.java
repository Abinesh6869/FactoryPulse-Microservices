package org.cts.fp_reporting.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "throughput_forecast")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThroughputForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long forecastId;

    private Long lineId;
    private String lineName;
    private String plantName;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    private int expectedUnits;
    private Double confidence;
}
