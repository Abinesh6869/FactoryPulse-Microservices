package org.cts.fp_identity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalNotificationRequest {
    private Long userId;
    private String employeeId;
    private String userName;
    private String message;
}
