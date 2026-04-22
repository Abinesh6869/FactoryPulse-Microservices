package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;
import org.cts.fp_identity.model.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class ShiftAllocationResponse {

    private Long allocationId;
    private Long shiftId;
    private String shiftName;
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long userId;
    private String userName;
    private String employeeId;
    private Role role;
    private Long allocatedById;
    private String allocatedByName;
    private LocalDateTime createdAt;
}
