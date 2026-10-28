package com.pro24_group4.pro24_group4.authorization;

import java.util.Collections;
import java.util.Set;

public record UserAuthoritySnapshot(
        String username,
        boolean deleted,
        boolean enabled,
        boolean accountNonLocked,
        boolean accountNonExpired,
        boolean credentialsNonExpired,
        Set<String> authorities
) {
    public UserAuthoritySnapshot {
        authorities = authorities == null ? Collections.emptySet() : Collections.unmodifiableSet(authorities);
    }

    public boolean isValid() {
        return !deleted && enabled && accountNonLocked && accountNonExpired && credentialsNonExpired;
    }
}
