package org.cts.fp_reporting.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LineInfo {
    private Long lineId;
    private String name;
    private Long plantId;
}
