package org.cts.fp_events.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {
    private Long userId;
    private String employeeId;
    private String name;       // mapped from fp_identity UserResponse.name
    private String status;
}
