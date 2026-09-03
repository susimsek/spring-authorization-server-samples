package io.github.susimsek.springauthserversamples.dto.admin;

import java.time.Instant;

public record AdminSigningKeySummaryDTO(
        String kid, String type, String algorithm, String use, Instant createdAt) {}
