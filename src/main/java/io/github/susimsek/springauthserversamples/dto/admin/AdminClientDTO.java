package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Schema(name = "AdminClient", description = "Registered OAuth2/OIDC client configuration.")
public record AdminClientDTO(
        @Schema(
                        description = "Internal client identifier.",
                        example = "b0a80123-4567-89ab-cdef-0123456789ab",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String id,
        @Schema(
                        description = "OAuth2 client identifier.",
                        example = "account-console",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String clientId,
        @Schema(
                        description = "Human-readable client name.",
                        example = "Account Console",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String clientName,
        @Schema(
                        description = "Client identifier issue time.",
                        example = "2026-09-04T08:30:00Z",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Instant clientIdIssuedAt,
        @Schema(
                        description = "Client-secret expiration time, if configured.",
                        example = "2027-09-04T08:30:00Z",
                        format = "date-time",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                Instant clientSecretExpiresAt,
        @Schema(
                        description = "Client authentication methods.",
                        example = "[\"none\"]",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Set<String> clientAuthenticationMethods,
        @Schema(
                        description = "Allowed OAuth2 grant types.",
                        example = "[\"authorization_code\", \"refresh_token\"]",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Set<String> authorizationGrantTypes,
        @Schema(
                        description = "Allowed authorization redirect URIs.",
                        example = "[\"http://localhost:3000/account/callback\"]",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Set<String> redirectUris,
        @Schema(
                        description = "Allowed post-logout redirect URIs.",
                        example = "[\"http://localhost:3000/account\"]",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Set<String> postLogoutRedirectUris,
        @Schema(
                        description = "Scopes that the client may request.",
                        example = "[\"openid\", \"account-api\"]",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Set<String> scopes,
        @Schema(
                        description = "Whether the user must approve requested scopes.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                boolean requireAuthorizationConsent,
        @Schema(
                        description = "Whether PKCE is required.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                boolean requireProofKey,
        @Schema(
                        description = "Authorization-code lifetime in ISO-8601 duration format.",
                        example = "PT5M",
                        format = "duration",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Duration authorizationCodeTimeToLive,
        @Schema(
                        description = "Access-token lifetime in ISO-8601 duration format.",
                        example = "PT5M",
                        format = "duration",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Duration accessTokenTimeToLive,
        @Schema(
                        description = "Refresh-token lifetime in ISO-8601 duration format.",
                        example = "PT8H",
                        format = "duration",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Duration refreshTokenTimeToLive) {}
