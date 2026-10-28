package com.pro24_group4.pro24_group4.controller;

import com.pro24_group4.pro24_group4.dto.permission.PermissionResponse;
import com.pro24_group4.pro24_group4.dto.user.CreateUserRequest;
import com.pro24_group4.pro24_group4.dto.user.UpdateUserRequest;
import com.pro24_group4.pro24_group4.dto.user.UserResponse;
import com.pro24_group4.pro24_group4.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse assignRole(@PathVariable Long userId, @PathVariable Long roleId) {
        return userService.assignRoleToUser(userId, roleId);
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse revokeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        return userService.revokeRoleFromUser(userId, roleId);
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('USER_READ')")
    public List<PermissionResponse> getUserPermissions(@PathVariable Long id) {
        return userService.getUserEffectivePermissions(id);
    }
}
