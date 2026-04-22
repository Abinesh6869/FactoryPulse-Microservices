package org.cts.fp_events.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
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
