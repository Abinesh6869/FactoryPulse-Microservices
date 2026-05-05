package org.cts.fp_events.dto.request;

import lombok.Data;

@Data
public class InternalNotificationRequest {
    private Long userId;
    private String employeeId;
    private String userName;
    private String message;
}
