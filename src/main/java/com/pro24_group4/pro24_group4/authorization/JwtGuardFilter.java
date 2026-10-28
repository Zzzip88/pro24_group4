package com.pro24_group4.pro24_group4.authorization;

import com.pro24_group4.pro24_group4.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class JwtGuardFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthorityLoader authorityLoader;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length())
                : uri;
        String normalizedPath = (path.length() > 1 && path.endsWith("/"))
                ? path.substring(0, path.length() - 1)
                : path;

        boolean isPost = "POST".equalsIgnoreCase(request.getMethod());
        boolean isPublicAuth = isPost && (
                normalizedPath.equals("/api/auth/register") ||
                normalizedPath.equals("/api/auth/login") ||
                normalizedPath.equals("/api/auth/refresh")
        );

        if (isPublicAuth) {
            filterChain.doFilter(new HideAuthorizationHeaderWrapper(request), response);
            return;
        }

        if (!normalizedPath.startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            errorResponseWriter.writeUnauthorized(response, uri, "Full authentication is required to access this resource");
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            errorResponseWriter.writeUnauthorized(response, uri, "Invalid or expired token");
            return;
        }

        String username;
        try {
            username = jwtService.extractUsername(token);
        } catch (Exception ex) {
            errorResponseWriter.writeUnauthorized(response, uri, "Invalid or expired token");
            return;
        }

        if (username == null || username.isBlank()) {
            errorResponseWriter.writeUnauthorized(response, uri, "Invalid or expired token");
            return;
        }

        Optional<UserAuthoritySnapshot> snapshotOpt = authorityLoader.find(username);
        if (snapshotOpt.isEmpty()) {
            errorResponseWriter.writeUnauthorized(response, uri, "User account not found");
            return;
        }

        UserAuthoritySnapshot snapshot = snapshotOpt.get();
        if (!snapshot.isValid()) {
            errorResponseWriter.writeUnauthorized(response, uri, "User account is inactive, locked, or deleted");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class HideAuthorizationHeaderWrapper extends HttpServletRequestWrapper {
        public HideAuthorizationHeaderWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return null;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return Collections.emptyEnumeration();
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Enumeration<String> baseNames = super.getHeaderNames();
            if (baseNames == null) {
                return Collections.emptyEnumeration();
            }
            List<String> names = Collections.list(baseNames);
            names.removeIf(n -> "Authorization".equalsIgnoreCase(n));
            return Collections.enumeration(names);
        }
    }
}
