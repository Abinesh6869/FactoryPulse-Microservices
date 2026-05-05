package org.cts.fp_telemetry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TelemetryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    private Long pointId;

    private String pointName;

    private Long machineId;

    private String machineName;

    private Long lineId;

    private String lineName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timeStamp;

    @UpdateTimestamp
    private LocalDateTime updatedTimeStamp;

    private String value;

    private String source;

    private String status;
}
