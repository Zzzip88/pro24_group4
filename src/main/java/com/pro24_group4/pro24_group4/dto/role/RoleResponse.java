package com.pro24_group4.pro24_group4.dto.role;

import com.pro24_group4.pro24_group4.dto.permission.PermissionResponse;

import java.time.LocalDateTime;
import java.util.List;

public record RoleResponse(
        Long id,
        String name,
        String description,
        List<PermissionResponse> permissions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
}
