package com.pro24_group4.pro24_group4.dto.permission;

import java.time.LocalDateTime;

public record PermissionResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
}
