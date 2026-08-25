package io.github.susimsek.springauthserversamples.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "One Spring Session-backed browser login.")
public record AccountSessionDTO(
        @Schema(example = "6f9b4dd0-2ed2-4af8-9e89-6ef3d4dd8c12") String id,
        Instant createdAt,
        Instant lastAccessedAt,
        Instant expiresAt,
        boolean current,
        List<AccountSessionClientDTO> clients) {}
