package org.cts.fp_identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[@#$%^&+=]).{6,}$",
            message = "Password must contain Atleast 1 Uppercase,1 Lowercase,1 number,1 Special Character")
    private String newPassword;
}
