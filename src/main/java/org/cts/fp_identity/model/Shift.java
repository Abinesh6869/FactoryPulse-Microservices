package org.cts.fp_identity.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shiftId;

    @ManyToOne
    @JoinColumn(name = "plant_id")
    private Plant plant;

    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate date;
}
