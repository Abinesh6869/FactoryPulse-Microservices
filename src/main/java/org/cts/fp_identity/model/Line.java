package org.cts.fp_identity.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Line {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lineId;

    @ManyToOne
    @JoinColumn(name = "plant_id")
    private Plant plant;

    private String name;
    private String productFamily;
    private String shiftPattern;
    private String status;
}
