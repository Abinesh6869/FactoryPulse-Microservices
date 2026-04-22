package org.cts.fp_events.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private Long userId;
    private String employeeId;
    private String userName;

    private Long alertId;

    private String channel;
    private String message;
    private LocalDateTime sentAt;
    private String status;
}
