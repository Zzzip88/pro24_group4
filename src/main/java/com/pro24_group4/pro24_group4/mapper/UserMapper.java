package com.pro24_group4.pro24_group4.mapper;

import com.pro24_group4.pro24_group4.dto.user.UserResponse;
import com.pro24_group4.pro24_group4.dto.user.UserRoleResponse;
import com.pro24_group4.pro24_group4.entity.Role;
import com.pro24_group4.pro24_group4.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoles")
    @Mapping(target = "enabled", expression = "java(user.getEnabled() != null ? user.getEnabled() : Boolean.TRUE)")
    @Mapping(target = "accountNonLocked", expression = "java(user.getAccountNonLocked() != null ? user.getAccountNonLocked() : Boolean.TRUE)")
    @Mapping(target = "accountNonExpired", expression = "java(user.getAccountNonExpired() != null ? user.getAccountNonExpired() : Boolean.TRUE)")
    @Mapping(target = "credentialsNonExpired", expression = "java(user.getCredentialsNonExpired() != null ? user.getCredentialsNonExpired() : Boolean.TRUE)")
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    @Named("mapRoles")
    default List<UserRoleResponse> mapRoles(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptyList();
        }
        return roles.stream()
                .filter(r -> r != null && r.getDeletedAt() == null)
                .map(r -> new UserRoleResponse(r.getId(), r.getName()))
                .sorted(Comparator.comparing(UserRoleResponse::name))
                .toList();
    }
}
