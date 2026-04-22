package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RootCauseResponse {
    private Long rootCauseId;
    private String code;
    private String description;
    private String category;
    private Long createdById;
    private String createdByEmployeeId;
    private String createdByName;
    private LocalDateTime createdAt;
}
