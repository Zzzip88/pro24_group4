package com.pro24_group4.pro24_group4.mapper;

import com.pro24_group4.pro24_group4.dto.permission.PermissionResponse;
import com.pro24_group4.pro24_group4.dto.role.RoleResponse;
import com.pro24_group4.pro24_group4.entity.Permission;
import com.pro24_group4.pro24_group4.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", source = "permissions", qualifiedByName = "mapPermissions")
    RoleResponse toResponse(Role role);

    List<RoleResponse> toResponseList(List<Role> roles);

    @Named("mapPermissions")
    default List<PermissionResponse> mapPermissions(Set<Permission> permissions) {
        if (permissions == null) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .filter(p -> p != null && p.getDeletedAt() == null)
                .map(p -> new PermissionResponse(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getCreatedAt(),
                        p.getUpdatedAt(),
                        p.getCreatedBy(),
                        p.getUpdatedBy()
                ))
                .sorted(Comparator.comparing(PermissionResponse::name))
                .toList();
    }
}
