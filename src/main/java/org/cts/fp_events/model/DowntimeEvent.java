package org.cts.fp_events.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DowntimeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long downtimeId;

    private Long lineId;
    private String lineName;

    private Long machineId;
    private String machineName;

    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long durationSec;
    private String category;

    private Long rootCauseId;
    private String rootCauseCode;
    private String rootCauseDescription;

    private Long loggedById;
    private String loggedByEmployeeId;
    private String loggedByName;

    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
