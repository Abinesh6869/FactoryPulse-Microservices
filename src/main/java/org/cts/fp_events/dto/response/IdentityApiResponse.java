package org.cts.fp_events.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentityApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
