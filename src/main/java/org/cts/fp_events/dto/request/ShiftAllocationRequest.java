package org.cts.fp_events.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class ShiftAllocationRequest {

    // Shift context for denormalization (client provides from fp_identity)
    private String shiftName;
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @NotEmpty(message = "At least one user is required")
    private List<AllocateeInfo> users;

    @Data
    public static class AllocateeInfo {
        private Long userId;
        private String employeeId;
        private String userName;
        private String role;
    }
}
