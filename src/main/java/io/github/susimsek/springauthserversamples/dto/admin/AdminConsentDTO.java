package io.github.susimsek.springauthserversamples.dto.admin;

import java.time.Instant;
import java.util.Set;

public record AdminConsentDTO(
        String clientId,
        String clientName,
        String principalName,
        Long userId,
        Set<String> authorities,
        Instant createdAt,
        Instant updatedAt) {}
