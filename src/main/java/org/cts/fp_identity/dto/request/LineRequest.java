package org.cts.fp_identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LineRequest {
    @NotNull(message = "Plant ID is required")
    private Long plantId;

    @NotBlank(message = "Name is required")
    private String name;

    private String productFamily;
    private String shiftPattern;
    private String status = "ACTIVE";
}
