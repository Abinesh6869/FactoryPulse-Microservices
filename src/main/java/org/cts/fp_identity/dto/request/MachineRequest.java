package org.cts.fp_identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MachineRequest {
    @NotNull(message = "Line ID is required")
    private Long lineId;

    @NotBlank(message = "Name is required")
    private String name;

    private String type;
    private String model;
    private String serialNumber;
    private LocalDate installDate;
    private String status = "ACTIVE";
}
