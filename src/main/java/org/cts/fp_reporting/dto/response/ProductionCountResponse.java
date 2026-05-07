package org.cts.fp_reporting.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductionCountResponse {
    private Long countId;
    private Long lineId;
    private String lineName;
    private Integer goodCount;
    private Integer rejectCount;
    private LocalDateTime timestamp;
}
