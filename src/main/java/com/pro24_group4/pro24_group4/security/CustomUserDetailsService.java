package com.pro24_group4.pro24_group4.security;

import com.pro24_group4.pro24_group4.entity.User;
import com.pro24_group4.pro24_group4.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(Boolean.FALSE.equals(user.getEnabled()))
                .accountExpired(Boolean.FALSE.equals(user.getAccountNonExpired()))
                .accountLocked(Boolean.FALSE.equals(user.getAccountNonLocked()))
                .credentialsExpired(Boolean.FALSE.equals(user.getCredentialsNonExpired()))
                .authorities("USER")
                .build();
    }
}
