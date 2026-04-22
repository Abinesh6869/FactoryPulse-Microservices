package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class ShiftResponse {
    private Long shiftId;
    private Long plantId;
    private String plantName;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate date;
}
