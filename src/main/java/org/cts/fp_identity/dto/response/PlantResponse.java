package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlantResponse {
    private Long plantId;
    private String name;
    private String location;
    private String timezone;
    private String status;
}
