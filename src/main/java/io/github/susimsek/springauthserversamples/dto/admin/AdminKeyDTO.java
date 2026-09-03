package io.github.susimsek.springauthserversamples.dto.admin;

import java.time.Instant;

public record AdminKeyDTO(
        String id,
        String kid,
        String type,
        String algorithm,
        String use,
        boolean active,
        Instant createdAt) {}
