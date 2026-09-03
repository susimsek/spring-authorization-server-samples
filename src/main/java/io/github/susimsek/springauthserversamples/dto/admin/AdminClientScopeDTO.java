package io.github.susimsek.springauthserversamples.dto.admin;

import java.time.Instant;

public record AdminClientScopeDTO(
        String id,
        String name,
        String displayName,
        String description,
        Instant createdAt,
        Instant updatedAt) {}
