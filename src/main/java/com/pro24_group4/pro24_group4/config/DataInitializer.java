
package com.pro24_group4.pro24_group4.config;

import com.pro24_group4.pro24_group4.authorization.Authorities;
import com.pro24_group4.pro24_group4.entity.Role;
import com.pro24_group4.pro24_group4.entity.User;
import com.pro24_group4.pro24_group4.repository.RoleRepository;
import com.pro24_group4.pro24_group4.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.dara-password:}")
    private String daraPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting default user initialization...");

        // Liquibase creates roles and permissions first
        // Realign sequences after manually inserting seed IDs
        realignSequences();

        Role adminRole = roleRepository.findByName(
                Authorities.ROLE_ADMIN
        ).orElseThrow(() ->
                new IllegalStateException(
                        "ROLE_ADMIN is missing. Check Liquibase seed data."
                )
        );

        Role userRole = roleRepository.findByName(
                Authorities.ROLE_USER
        ).orElseThrow(() ->
                new IllegalStateException(
                        "ROLE_USER is missing. Check Liquibase seed data."
                )
        );

        // Create the default admin account if absent.
        if (userRepository.findByUsername("admin").isEmpty()) {
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn(
                        "Admin password is empty. Skipping admin creation."
                );
            } else {
                User admin = User.builder()
                        .username("admin")
                        .nickName("Admin")
                        .passwordHash(passwordEncoder.encode(adminPassword))
                        .enabled(true)
                        .accountNonLocked(true)
                        .accountNonExpired(true)
                        .credentialsNonExpired(true)
                        .roles(new HashSet<>(Set.of(adminRole)))
                        .build();

                userRepository.save(admin);
                log.info("Initialized default admin user.");
            }
        } else {
            log.info("Admin user already exists. Skipping creation.");
        }

        // Create Dara only if this account is required by your group.
        if (userRepository.findByUsername("dara").isEmpty()) {
            if (daraPassword == null || daraPassword.isBlank()) {
                log.warn(
                        "Dara password is empty. Skipping Dara creation."
                );
            } else {
                User dara = User.builder()
                        .username("dara")
                        .nickName("Dara")
                        .passwordHash(passwordEncoder.encode(daraPassword))
                        .enabled(true)
                        .accountNonLocked(true)
                        .accountNonExpired(true)
                        .credentialsNonExpired(true)
                        .roles(new HashSet<>(Set.of(userRole)))
                        .build();

                userRepository.save(dara);
                log.info("Initialized default Dara user.");
            }
        } else {
            log.info("Dara user already exists. Skipping creation.");
        }

        realignSequences();
        log.info("Default user initialization completed.");
    }

    private void realignSequences() {
        realignSequence("roles", "id");
        realignSequence("permissions", "id");
        realignSequence("users", "id");
    }

    private void realignSequence(String table, String column) {
        String sql = String.format(
                "SELECT setval(pg_get_serial_sequence('%s', '%s'), " +
                        "COALESCE((SELECT MAX(%s) FROM %s), 1), " +
                        "(SELECT MAX(%s) IS NOT NULL FROM %s))",
                table, column, column, table, column, table
        );

        try {
            jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception ex) {
            log.warn(
                    "Could not realign sequence for {}.{}: {}",
                    table, column, ex.getMessage()
            );
        }
    }
}
