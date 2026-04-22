package org.cts.fp_identity.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ShiftAllocationRequest {

    @NotEmpty(message = "At least one employee ID is required")
    private List<String> employeeIds;
}
