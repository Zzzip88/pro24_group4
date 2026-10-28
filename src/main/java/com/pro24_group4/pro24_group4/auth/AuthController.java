package com.pro24_group4.pro24_group4.auth;

import com.pro24_group4.pro24_group4.auth.dto.AuthResponse;
import com.pro24_group4.pro24_group4.auth.dto.LoginRequest;
import com.pro24_group4.pro24_group4.auth.dto.RegisterRequest;
import com.pro24_group4.pro24_group4.auth.dto.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }
    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {

        return Map.of(
                "username",
                authentication.getName()
        );
    }
    @PostMapping("/refresh")
    public AuthResponse refresh(
            @RequestBody RefreshRequest request) {

        return authService.refresh(request);
    }
}
