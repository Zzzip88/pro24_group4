package com.pro24_group4.pro24_group4.controller;

import com.pro24_group4.pro24_group4.dto.permission.CreatePermissionRequest;
import com.pro24_group4.pro24_group4.dto.permission.PermissionResponse;
import com.pro24_group4.pro24_group4.dto.permission.UpdatePermissionRequest;
import com.pro24_group4.pro24_group4.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    public List<PermissionResponse> getAllPermissions() {
        return permissionService.getAllPermissions();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    public PermissionResponse getPermissionById(@PathVariable Long id) {
        return permissionService.getPermissionById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERMISSION_WRITE')")
    public PermissionResponse createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        return permissionService.createPermission(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_WRITE')")
    public PermissionResponse updatePermission(@PathVariable Long id, @Valid @RequestBody UpdatePermissionRequest request) {
        return permissionService.updatePermission(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    public void deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
    }
}
