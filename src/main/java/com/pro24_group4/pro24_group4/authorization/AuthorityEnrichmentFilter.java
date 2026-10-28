package com.pro24_group4.pro24_group4.authorization;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class AuthorityEnrichmentFilter extends OncePerRequestFilter {

    private final AuthorityLoader authorityLoader;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

        if (currentAuth != null && currentAuth.isAuthenticated() && !(currentAuth instanceof AnonymousAuthenticationToken)) {
            String username = currentAuth.getName();
            Optional<UserAuthoritySnapshot> snapshotOpt = authorityLoader.find(username);

            if (snapshotOpt.isPresent()) {
                UserAuthoritySnapshot snapshot = snapshotOpt.get();
                List<SimpleGrantedAuthority> authorities = snapshot.authorities().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                UsernamePasswordAuthenticationToken enrichedAuth = new UsernamePasswordAuthenticationToken(
                        currentAuth.getPrincipal(),
                        currentAuth.getCredentials(),
                        authorities
                );
                enrichedAuth.setDetails(currentAuth.getDetails());
                SecurityContextHolder.getContext().setAuthentication(enrichedAuth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
