package org.cts.fp_maintenance.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workOrderId;

    private Long machineId;
    private String machineName;

    private Long downtimeId;

    private Long createdById;
    private String createdByName;
    private String createdByEmployeeId;

    private String priority;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long assignedToId;
    private String assignedToName;
    private String assignedToEmployeeId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private String status;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
