package org.cts.fp_identity.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MachineDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long docId;

    @ManyToOne
    @JoinColumn(name = "machine_id")
    private Machine machine;

    private String docType;
    private String fileUri;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    private Boolean verified;
}
