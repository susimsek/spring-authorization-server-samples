package io.github.susimsek.springauthserversamples.dto.admin;

import java.time.Instant;
import java.util.Set;

public record AdminUserDTO(
        Long id,
        String username,
        String email,
        boolean emailVerified,
        boolean enabled,
        String avatarUrl,
        Set<String> authorities,
        Instant createdAt,
        Instant updatedAt) {

    public AdminUserDTO(
            Long id,
            String username,
            boolean enabled,
            String avatarUrl,
            Set<String> authorities,
            Instant createdAt,
            Instant updatedAt) {
        this(id, username, null, false, enabled, avatarUrl, authorities, createdAt, updatedAt);
    }
}
