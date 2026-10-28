package com.pro24_group4.pro24_group4.service;

import com.pro24_group4.pro24_group4.dto.permission.PermissionResponse;
import com.pro24_group4.pro24_group4.dto.user.CreateUserRequest;
import com.pro24_group4.pro24_group4.dto.user.UpdateUserRequest;
import com.pro24_group4.pro24_group4.dto.user.UserResponse;
import com.pro24_group4.pro24_group4.entity.Permission;
import com.pro24_group4.pro24_group4.entity.Role;
import com.pro24_group4.pro24_group4.entity.User;
import com.pro24_group4.pro24_group4.exception.DuplicateResourceException;
import com.pro24_group4.pro24_group4.exception.ResourceNotFoundException;
import com.pro24_group4.pro24_group4.mapper.PermissionMapper;
import com.pro24_group4.pro24_group4.mapper.UserMapper;
import com.pro24_group4.pro24_group4.repository.RoleManagementRepository;
import com.pro24_group4.pro24_group4.repository.RoleRepository;
import com.pro24_group4.pro24_group4.repository.UserManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserManagementRepository userManagementRepository;
    private final RoleManagementRepository roleManagementRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;

    public List<UserResponse> getAllUsers() {
        return userManagementRepository.findAllByDeletedAtIsNull().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userManagementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userManagementRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_USER not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickName(request.nickName())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .roles(roles)
                .build();

        user = userManagementRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userManagementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.nickName() != null) {
            user.setNickName(request.nickName());
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        if (request.accountNonLocked() != null) {
            user.setAccountNonLocked(request.accountNonLocked());
        }

        if (request.accountNonExpired() != null) {
            user.setAccountNonExpired(request.accountNonExpired());
        }

        if (request.credentialsNonExpired() != null) {
            user.setCredentialsNonExpired(request.credentialsNonExpired());
        }

        user = userManagementRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userManagementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setDeletedAt(LocalDateTime.now());
        userManagementRepository.save(user);
    }

    @Transactional
    public UserResponse assignRoleToUser(Long userId, Long roleId) {
        User user = userManagementRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleManagementRepository.findByIdAndDeletedAtIsNull(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        boolean alreadyAssigned = user.getRoles().stream()
                .anyMatch(r -> r.getId().equals(roleId));

        if (alreadyAssigned) {
            throw new DuplicateResourceException("Role already assigned to user");
        }

        user.getRoles().add(role);
        user = userManagementRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse revokeRoleFromUser(Long userId, Long roleId) {
        User user = userManagementRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            throw new ResourceNotFoundException("Role is not assigned to user");
        }

        boolean removed = user.getRoles().removeIf(r -> r.getId().equals(roleId));
        if (!removed) {
            throw new ResourceNotFoundException("Role is not assigned to user");
        }

        user = userManagementRepository.save(user);
        return userMapper.toResponse(user);
    }

    public List<PermissionResponse> getUserEffectivePermissions(Long userId) {
        User user = userManagementRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRoles() == null) {
            return Collections.emptyList();
        }

        return user.getRoles().stream()
                .filter(r -> r != null && r.getDeletedAt() == null && r.getPermissions() != null)
                .flatMap(r -> r.getPermissions().stream())
                .filter(p -> p != null && p.getDeletedAt() == null)
                .collect(Collectors.toMap(
                        Permission::getId,
                        p -> p,
                        (existing, replacement) -> existing
                ))
                .values().stream()
                .map(permissionMapper::toResponse)
                .sorted(Comparator.comparing(PermissionResponse::name))
                .toList();
    }
}
