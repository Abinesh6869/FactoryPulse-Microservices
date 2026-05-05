package org.cts.fp_reporting.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkOrderResponse {
    private Long workOrderId;
    private String status;
    private LocalDateTime createdAt;
}
