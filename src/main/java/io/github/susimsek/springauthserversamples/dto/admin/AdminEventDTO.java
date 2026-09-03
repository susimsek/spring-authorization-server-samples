package io.github.susimsek.springauthserversamples.dto.admin;

import java.time.Instant;

public record AdminEventDTO(
        String id,
        String actor,
        String action,
        String targetType,
        String targetId,
        Instant occurredAt) {}
