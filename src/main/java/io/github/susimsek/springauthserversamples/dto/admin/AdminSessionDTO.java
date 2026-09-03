package io.github.susimsek.springauthserversamples.dto.admin;

import java.time.Instant;

public record AdminSessionDTO(
        String id,
        String username,
        Instant createdAt,
        Instant lastAccessedAt,
        Instant expiresAt,
        long authorizationCount,
        boolean active) {

    public AdminSessionDTO(
            String id,
            String username,
            Instant createdAt,
            Instant lastAccessedAt,
            Instant expiresAt,
            long authorizationCount) {
        this(id, username, createdAt, lastAccessedAt, expiresAt, authorizationCount, true);
    }
}
