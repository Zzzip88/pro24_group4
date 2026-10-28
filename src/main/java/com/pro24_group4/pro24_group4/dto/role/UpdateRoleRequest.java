package com.pro24_group4.pro24_group4.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^ROLE_[A-Z0-9_]+$", message = "Role name must start with 'ROLE_' followed by uppercase letters, numbers, or underscores")
        String name,

        @Size(max = 255)
        String description
) {
}
