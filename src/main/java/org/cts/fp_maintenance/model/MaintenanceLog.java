package org.cts.fp_maintenance.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    private Long workOrderId;
    private Long machineId;
    private String machineName;

    private Long performedById;
    private String performedByName;
    private String performedByEmployeeId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime performedAt;

    private String notes;

    @Column(name = "parts_used_json", columnDefinition = "TEXT")
    private String partsUsedJson;

    private Integer timeSpentMinutes;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
