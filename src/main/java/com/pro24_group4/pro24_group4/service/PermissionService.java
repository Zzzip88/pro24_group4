package com.pro24_group4.pro24_group4.service;

import com.pro24_group4.pro24_group4.dto.permission.CreatePermissionRequest;
import com.pro24_group4.pro24_group4.dto.permission.PermissionResponse;
import com.pro24_group4.pro24_group4.dto.permission.UpdatePermissionRequest;
import com.pro24_group4.pro24_group4.entity.Permission;
import com.pro24_group4.pro24_group4.exception.DuplicateResourceException;
import com.pro24_group4.pro24_group4.exception.ResourceNotFoundException;
import com.pro24_group4.pro24_group4.mapper.PermissionMapper;
import com.pro24_group4.pro24_group4.repository.PermissionManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionManagementRepository permissionManagementRepository;
    private final PermissionMapper permissionMapper;

    public List<PermissionResponse> getAllPermissions() {
        return permissionManagementRepository.findAllByDeletedAtIsNull().stream()
                .map(permissionMapper::toResponse)
                .toList();
    }

    public PermissionResponse getPermissionById(Long id) {
        Permission permission = permissionManagementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));
        return permissionMapper.toResponse(permission);
    }

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionManagementRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Permission name already exists: " + request.name());
        }

        Permission permission = Permission.builder()
                .name(request.name())
                .description(request.description())
                .build();

        permission = permissionManagementRepository.save(permission);
        return permissionMapper.toResponse(permission);
    }

    @Transactional
    public PermissionResponse updatePermission(Long id, UpdatePermissionRequest request) {
        Permission permission = permissionManagementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));

        if (permissionManagementRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException("Permission name already exists: " + request.name());
        }

        permission.setName(request.name());
        permission.setDescription(request.description());

        permission = permissionManagementRepository.save(permission);
        return permissionMapper.toResponse(permission);
    }

    @Transactional
    public void deletePermission(Long id) {
        Permission permission = permissionManagementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));

        permission.setDeletedAt(LocalDateTime.now());
        permissionManagementRepository.save(permission);
    }
}
