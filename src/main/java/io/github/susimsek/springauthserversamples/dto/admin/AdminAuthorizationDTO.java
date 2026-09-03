package io.github.susimsek.springauthserversamples.dto.admin;

import java.time.Instant;
import java.util.List;

public record AdminAuthorizationDTO(
        String id,
        String clientId,
        String clientName,
        String grantType,
        List<String> scopes,
        Instant accessTokenIssuedAt,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt) {}
