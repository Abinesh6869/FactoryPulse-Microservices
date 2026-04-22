package org.cts.fp_identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlantRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Timezone is required")
    private String timezone;
    private String status = "ACTIVE";
}
