package com.pro24_group4.pro24_group4.controller;

import com.pro24_group4.pro24_group4.dto.role.CreateRoleRequest;
import com.pro24_group4.pro24_group4.dto.role.RoleResponse;
import com.pro24_group4.pro24_group4.dto.role.UpdateRoleRequest;
import com.pro24_group4.pro24_group4.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public List<RoleResponse> getAllRoles() {
        return roleService.getAllRoles();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public RoleResponse getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public RoleResponse createRole(@Valid @RequestBody CreateRoleRequest request) {
        return roleService.createRole(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public RoleResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return roleService.updateRole(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public void deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoleResponse assignPermission(@PathVariable Long roleId, @PathVariable Long permissionId) {
        return roleService.assignPermissionToRole(roleId, permissionId);
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoleResponse revokePermission(@PathVariable Long roleId, @PathVariable Long permissionId) {
        return roleService.revokePermissionFromRole(roleId, permissionId);
    }
}
