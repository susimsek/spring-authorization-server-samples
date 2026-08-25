package io.github.susimsek.springauthserversamples.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;

@Schema(description = "An OAuth2/OIDC client authorized by the authenticated account.")
public record AccountApplicationDTO(
        @Schema(example = "account-console") String clientId,
        @Schema(example = "Account Console") String clientName,
        Set<String> scopes,
        Instant createdAt,
        Instant updatedAt) {}
