package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MachineDocumentResponse {
    private Long docId;
    private Long machineId;
    private String name;
    private String docType;
    private String fileUri;
    private LocalDateTime uploadedAt;
    private Boolean verified;
}
