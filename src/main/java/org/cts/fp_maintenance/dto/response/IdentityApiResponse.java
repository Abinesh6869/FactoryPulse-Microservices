package org.cts.fp_maintenance.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentityApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
