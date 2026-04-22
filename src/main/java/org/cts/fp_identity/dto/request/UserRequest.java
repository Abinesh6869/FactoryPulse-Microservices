package org.cts.fp_identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.cts.fp_identity.model.Role;

@Data
public class UserRequest {
    @NotBlank(message = "Name is required")
    @Pattern(regexp = "^[A-Za-z ]{3,20}$", message = "Name must contain only letters (3-20 chars)")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[@#$%^&+=]).{6,}$",
            message = "Password must have at least 1 uppercase, 1 special character, min 6 chars")
    private String password;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    private Role role;
    private String status = "ACTIVE";
}
