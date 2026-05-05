package org.cts.fp_identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlantRequest {
    @NotBlank(message = "Plant name is required")
    @Size(max = 100, message = "Plant name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Timezone is required")
    private String timezone;
    private String status = "ACTIVE";
}
