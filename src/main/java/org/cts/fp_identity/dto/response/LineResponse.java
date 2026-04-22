package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LineResponse {
    private Long lineId;
    private Long plantId;
    private String plantName;
    private String name;
    private String productFamily;
    private String shiftPattern;
    private String status;
}
