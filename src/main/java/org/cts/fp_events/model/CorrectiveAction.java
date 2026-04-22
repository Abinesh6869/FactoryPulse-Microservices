package org.cts.fp_events.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CorrectiveAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionId;

    private Long downtimeId;
    private String machineName;
    private String lineName;
    private String rootCauseCode;

    private Long assignedToId;
    private String assignedToEmployeeId;
    private String assignedToName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dueDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
    private String status;
}
