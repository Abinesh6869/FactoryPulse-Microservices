package org.cts.fp_reporting.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiftInfo {
    private Long shiftId;
    private String name;
    private Long plantId;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate date;
}
