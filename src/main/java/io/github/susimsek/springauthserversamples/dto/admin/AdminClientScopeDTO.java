package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "AdminClientScope", description = "OAuth2 client scope definition.")
public record AdminClientScopeDTO(
        @Schema(
                        description = "Internal scope identifier.",
                        example = "scope-123",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String id,
        @Schema(
                        description = "Machine-readable scope name.",
                        example = "account-api",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String name,
        @Schema(
                        description = "Optional display name.",
                        example = "Account API",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String displayName,
        @Schema(
                        description = "Optional scope description.",
                        example = "Read and manage the current account.",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String description,
        @Schema(
                        description = "Creation time.",
                        example = "2026-09-04T08:30:00Z",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Instant createdAt,
        @Schema(
                        description = "Last update time.",
                        example = "2026-09-04T08:30:00Z",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Instant updatedAt,
        @Schema(
                        description = "Whether this scope emits group membership claims.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                boolean groupMapperEnabled,
        @Schema(
                        description = "Claim name used for mapped groups.",
                        example = "groups",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String groupClaimName,
        @Schema(
                        description = "Whether group claims contain full hierarchical paths.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                boolean groupMapperFullPath) {

    public AdminClientScopeDTO(
            String id,
            String name,
            String displayName,
            String description,
            Instant createdAt,
            Instant updatedAt) {
        this(id, name, displayName, description, createdAt, updatedAt, false, "groups", true);
    }
}
