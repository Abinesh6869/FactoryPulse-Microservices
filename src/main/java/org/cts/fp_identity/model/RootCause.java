package org.cts.fp_identity.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RootCause {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rootCauseId;

    private String code;
    private String description;
    private String category;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
