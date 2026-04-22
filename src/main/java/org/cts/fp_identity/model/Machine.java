package org.cts.fp_identity.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Machine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long machineId;

    @ManyToOne
    @JoinColumn(name = "line_id")
    private Line line;

    private String name;
    private String type;
    private String model;
    private String serialNumber;
    private LocalDate installDate;
    private String status;
}
