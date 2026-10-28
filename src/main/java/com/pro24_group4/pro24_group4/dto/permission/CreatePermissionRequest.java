package com.pro24_group4.pro24_group4.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePermissionRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Permission name must start with an uppercase letter and contain only uppercase letters, numbers, and underscores")
        String name,

        @Size(max = 255)
        String description
) {
}
