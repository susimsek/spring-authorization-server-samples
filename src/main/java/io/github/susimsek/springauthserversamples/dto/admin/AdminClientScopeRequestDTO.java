package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
        name = "AdminClientScopeRequest",
        description = "Client scope definition to create or update.")
public record AdminClientScopeRequestDTO(
        @Schema(
                        description = "Machine-readable scope name.",
                        example = "reporting-api",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = 100)
                String name,
        @Schema(
                        description = "Optional display name.",
                        example = "Reporting API",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Size(max = 200)
                String displayName,
        @Schema(
                        description = "Optional scope description.",
                        example = "Read reporting data.",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Size(max = 500)
                String description) {}
