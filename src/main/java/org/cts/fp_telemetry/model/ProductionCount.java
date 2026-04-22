package org.cts.fp_telemetry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductionCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long countId;

    private Long lineId;

    private String lineName;

    private Long shiftId;

    private String shiftName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timeStamp;

    @UpdateTimestamp
    private LocalDateTime updatedTimeStamp;

    private Integer goodCount;

    private Integer rejectCount;
}
