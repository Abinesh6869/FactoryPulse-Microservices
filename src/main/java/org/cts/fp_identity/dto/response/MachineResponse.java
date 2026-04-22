package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MachineResponse {
    private Long machineId;
    private Long lineId;
    private String lineName;
    private Long plantId;
    private String plantName;
    private String name;
    private String type;
    private String model;
    private String serialNumber;
    private LocalDate installDate;
    private String status;
}
