package com.pro24_group4.pro24_group4.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100)
        String nickName,

        @Size(min = 8, max = 72)
        String password,

        Boolean enabled,

        Boolean accountNonLocked,

        Boolean accountNonExpired,

        Boolean credentialsNonExpired
) {
}
