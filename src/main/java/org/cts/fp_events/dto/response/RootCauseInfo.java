package org.cts.fp_events.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RootCauseInfo {
    private Long rootCauseId;
    private String code;
    private String description;
    private String category;
}
