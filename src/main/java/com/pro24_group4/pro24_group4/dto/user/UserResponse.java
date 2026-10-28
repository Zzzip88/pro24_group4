package com.pro24_group4.pro24_group4.dto.user;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String nickName,
        Boolean enabled,
        Boolean accountNonLocked,
        Boolean accountNonExpired,
        Boolean credentialsNonExpired,
        List<UserRoleResponse> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
}
