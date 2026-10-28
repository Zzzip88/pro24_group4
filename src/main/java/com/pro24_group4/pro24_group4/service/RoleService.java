package com.pro24_group4.pro24_group4.service;

import com.pro24_group4.pro24_group4.dto.role.CreateRoleRequest;
import com.pro24_group4.pro24_group4.dto.role.RoleResponse;
import com.pro24_group4.pro24_group4.dto.role.UpdateRoleRequest;
import com.pro24_group4.pro24_group4.entity.Permission;
import com.pro24_group4.pro24_group4.entity.Role;
import com.pro24_group4.pro24_group4.exception.BusinessRuleException;
import com.pro24_group4.pro24_group4.exception.DuplicateResourceException;
import com.pro24_group4.pro24_group4.exception.ResourceNotFoundException;
import com.pro24_group4.pro24_group4.mapper.RoleMapper;
import com.pro24_group4.pro24_group4.repository.PermissionManagementRepository;
import com.pro24_group4.pro24_group4.repository.RoleManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleManagementRepository roleManagementRepository;
    private final PermissionManagementRepository permissionManagementRepository;
    private final RoleMapper roleMapper;

    public List<RoleResponse> getAllRoles() {
        return roleManagementRepository.findAllByDeletedAtIsNull().stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    public RoleResponse getRoleById(Long id) {
        Role role = roleManagementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return roleMapper.toResponse(role);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleManagementRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Role name already exists: " + request.name());
        }

        Role role = Role.builder()
                .name(request.name())
                .description(request.description())
                .permissions(new HashSet<>())
                .build();

        role = roleManagementRepository.save(role);
        return roleMapper.toResponse(role);
    }

    @Transactional
    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        Role role = roleManagementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        if (isReservedRole(role.getName()) && !role.getName().equals(request.name())) {
            throw new BusinessRuleException("Cannot rename reserved role: " + role.getName());
        }

        if (roleManagementRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException("Role name already exists: " + request.name());
        }

        role.setName(request.name());
        role.setDescription(request.description());

        role = roleManagementRepository.save(role);
        return roleMapper.toResponse(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleManagementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        if (isReservedRole(role.getName())) {
            throw new BusinessRuleException("Cannot delete reserved role: " + role.getName());
        }

        role.setDeletedAt(LocalDateTime.now());
        roleManagementRepository.save(role);
    }

    @Transactional
    public RoleResponse assignPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleManagementRepository.findByIdAndDeletedAtIsNull(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        Permission permission = permissionManagementRepository.findByIdAndDeletedAtIsNull(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + permissionId));

        if (role.getPermissions() == null) {
            role.setPermissions(new HashSet<>());
        }

        boolean alreadyAssigned = role.getPermissions().stream()
                .anyMatch(p -> p.getId().equals(permissionId));

        if (alreadyAssigned) {
            throw new DuplicateResourceException("Permission already assigned to role");
        }

        role.getPermissions().add(permission);
        role = roleManagementRepository.save(role);
        return roleMapper.toResponse(role);
    }

    @Transactional
    public RoleResponse revokePermissionFromRole(Long roleId, Long permissionId) {
        Role role = roleManagementRepository.findByIdAndDeletedAtIsNull(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
            throw new ResourceNotFoundException("Permission is not assigned to role");
        }

        boolean removed = role.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        if (!removed) {
            throw new ResourceNotFoundException("Permission is not assigned to role");
        }

        role = roleManagementRepository.save(role);
        return roleMapper.toResponse(role);
    }

    private boolean isReservedRole(String name) {
        return "ROLE_ADMIN".equals(name) || "ROLE_USER".equals(name);
    }
}
