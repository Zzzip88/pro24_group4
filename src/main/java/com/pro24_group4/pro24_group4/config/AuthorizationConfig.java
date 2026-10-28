package com.pro24_group4.pro24_group4.config;

import com.pro24_group4.pro24_group4.authorization.AuthorityEnrichmentFilter;
import com.pro24_group4.pro24_group4.authorization.AuthorityLoader;
import com.pro24_group4.pro24_group4.authorization.ErrorResponseWriter;
import com.pro24_group4.pro24_group4.authorization.JwtGuardFilter;
import com.pro24_group4.pro24_group4.security.JwtService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class AuthorizationConfig {

    @Bean
    public ErrorResponseWriter errorResponseWriter() {
        return new ErrorResponseWriter();
    }

    @Bean
    public FilterRegistrationBean<JwtGuardFilter> jwtGuardFilterRegistration(
            JwtService jwtService,
            @Lazy AuthorityLoader authorityLoader,
            ErrorResponseWriter errorResponseWriter
    ) {
        JwtGuardFilter filter = new JwtGuardFilter(jwtService, authorityLoader, errorResponseWriter);
        FilterRegistrationBean<JwtGuardFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(-101);
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AuthorityEnrichmentFilter> authorityEnrichmentFilterRegistration(
            @Lazy AuthorityLoader authorityLoader
    ) {
        AuthorityEnrichmentFilter filter = new AuthorityEnrichmentFilter(authorityLoader);
        FilterRegistrationBean<AuthorityEnrichmentFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(-99);
        registration.addUrlPatterns("/api/*");
        return registration;
    }
}
