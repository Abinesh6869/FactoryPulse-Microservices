package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ShiftAllocationSummaryResponse {
    private List<ShiftAllocationResponse> allocated;
    private List<String> skipped;
    private List<String> notFound;
    private List<String> invalidRole;
}
