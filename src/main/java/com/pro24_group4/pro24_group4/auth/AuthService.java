package com.pro24_group4.pro24_group4.auth;

import com.pro24_group4.pro24_group4.auth.dto.AuthResponse;
import com.pro24_group4.pro24_group4.auth.dto.LoginRequest;
import com.pro24_group4.pro24_group4.auth.dto.RegisterRequest;
import com.pro24_group4.pro24_group4.auth.dto.RefreshRequest;
import com.pro24_group4.pro24_group4.entity.User;
import com.pro24_group4.pro24_group4.repository.UserRepository;
import com.pro24_group4.pro24_group4.entity.Role;
import com.pro24_group4.pro24_group4.repository.RoleRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import com.pro24_group4.pro24_group4.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        Role userRole = roleRepository
                .findByName("ROLE_USER")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "ROLE_USER not found"
                        ));
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());

        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        String token = jwtService.generateToken(
                request.getUsername()
        );

        return new AuthResponse(token);
    }
    public AuthResponse refresh(RefreshRequest request) {

        String username = jwtService.extractUsername(request.getToken());

        String newToken = jwtService.generateToken(username);

        return new AuthResponse(newToken);
    }
}
