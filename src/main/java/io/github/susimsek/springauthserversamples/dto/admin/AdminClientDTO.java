package io.github.susimsek.springauthserversamples.dto.admin;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public record AdminClientDTO(
        String id,
        String clientId,
        String clientName,
        Instant clientIdIssuedAt,
        Instant clientSecretExpiresAt,
        Set<String> clientAuthenticationMethods,
        Set<String> authorizationGrantTypes,
        Set<String> redirectUris,
        Set<String> postLogoutRedirectUris,
        Set<String> scopes,
        boolean requireAuthorizationConsent,
        boolean requireProofKey,
        Duration authorizationCodeTimeToLive,
        Duration accessTokenTimeToLive,
        Duration refreshTokenTimeToLive) {}
