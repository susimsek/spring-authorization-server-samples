package io.github.susimsek.springauthserversamples.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SocialProvider", description = "Social login provider availability.")
public record SocialProviderDTO(
        @Schema(
                        description = "Social provider registration identifier.",
                        example = "microsoft",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String provider,
        @Schema(
                        description =
                                "Whether the provider has a usable Client ID and Client Secret.",
                        example = "false",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                boolean configured) {}
