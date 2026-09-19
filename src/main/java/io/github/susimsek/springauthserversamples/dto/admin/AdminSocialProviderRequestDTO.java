package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
        name = "AdminSocialProviderRequest",
        description = "Social login provider credentials update.")
public record AdminSocialProviderRequestDTO(
        @Schema(
                        description = "Provider registration id.",
                        example = "google",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                String provider,
        @Schema(
                        description = "OAuth client id.",
                        example = "client-id",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = 500)
                String clientId,
        @Schema(
                        description =
                                "New OAuth client secret. Leave blank to keep the existing secret.",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true)
                @Size(max = 1000)
                String clientSecret) {}
