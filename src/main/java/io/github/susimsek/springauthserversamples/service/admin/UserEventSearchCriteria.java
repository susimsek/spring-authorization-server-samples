package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.UserEventType;
import java.time.Instant;

public record UserEventSearchCriteria(
        String query,
        UserEventType type,
        String username,
        String clientId,
        String ipAddress,
        Instant from,
        Instant to) {}
