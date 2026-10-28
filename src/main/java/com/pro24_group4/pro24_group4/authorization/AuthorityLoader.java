package com.pro24_group4.pro24_group4.authorization;

import com.pro24_group4.pro24_group4.entity.Permission;
import com.pro24_group4.pro24_group4.entity.Role;
import com.pro24_group4.pro24_group4.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthorityLoader {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Optional<UserAuthoritySnapshot> find(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        List<User> results = entityManager.createQuery(
                        "SELECT DISTINCT u FROM User u " +
                        "LEFT JOIN FETCH u.roles r " +
                        "LEFT JOIN FETCH r.permissions p " +
                        "WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        User user = results.get(0);

        boolean deleted = user.getDeletedAt() != null;
        boolean enabled = user.getEnabled() == null || user.getEnabled();
        boolean accountNonLocked = user.getAccountNonLocked() == null || user.getAccountNonLocked();
        boolean accountNonExpired = user.getAccountNonExpired() == null || user.getAccountNonExpired();
        boolean credentialsNonExpired = user.getCredentialsNonExpired() == null || user.getCredentialsNonExpired();

        Set<String> authorities = new HashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role != null && role.getDeletedAt() == null) {
                    if (role.getName() != null && !role.getName().isBlank()) {
                        authorities.add(role.getName());
                    }
                    if (role.getPermissions() != null) {
                        for (Permission perm : role.getPermissions()) {
                            if (perm != null && perm.getDeletedAt() == null) {
                                if (perm.getName() != null && !perm.getName().isBlank()) {
                                    authorities.add(perm.getName());
                                }
                            }
                        }
                    }
                }
            }
        }

        return Optional.of(new UserAuthoritySnapshot(
                user.getUsername(),
                deleted,
                enabled,
                accountNonLocked,
                accountNonExpired,
                credentialsNonExpired,
                authorities
        ));
    }
}
