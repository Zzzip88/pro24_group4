package com.pro24_group4.pro24_group4.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank
        @Size(min = 3, max = 100)
        String username,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @Size(max = 100)
        String nickName
) {
}
