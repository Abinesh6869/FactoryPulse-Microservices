package org.cts.fp_identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RootCauseRequest {
    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;
}
