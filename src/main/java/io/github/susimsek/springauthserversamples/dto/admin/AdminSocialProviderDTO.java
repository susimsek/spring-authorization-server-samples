package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminSocialProvider", description = "Social login provider credentials status.")
public record AdminSocialProviderDTO(
        @Schema(
                        description = "Provider registration id.",
                        example = "google",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String provider,
        @Schema(
                        description = "OAuth client id.",
                        example = "client-id",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String clientId,
        @Schema(
                        description = "Whether a client secret is configured.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                boolean clientSecretConfigured) {}
