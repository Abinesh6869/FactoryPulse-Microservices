package org.cts.fp_identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MachineDocumentRequest {
    @NotNull(message = "Machine ID is required")
    private Long machineId;

    @NotBlank(message = "Document type is required")
    private String docType;

    @NotBlank(message = "File URI is required")
    private String fileUri;


}
