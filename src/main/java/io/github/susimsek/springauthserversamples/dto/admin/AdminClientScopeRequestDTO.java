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
                String description,
        @Schema(
                        description = "Enable the allow-listed group membership mapper.",
                        example = "false",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                Boolean groupMapperEnabled,
        @Schema(
                        description = "Claim name used for mapped groups.",
                        example = "groups",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Size(max = 100)
                String groupClaimName,
        @Schema(
                        description = "Emit full hierarchical group paths.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                Boolean groupMapperFullPath) {

    public AdminClientScopeRequestDTO(String name, String displayName, String description) {
        this(name, displayName, description, false, "groups", true);
    }

    public boolean groupMapperEnabledValue() {
        return Boolean.TRUE.equals(groupMapperEnabled);
    }

    public boolean groupMapperFullPathValue() {
        return groupMapperFullPath == null || groupMapperFullPath;
    }

    public String groupClaimNameValue() {
        return groupClaimName == null || groupClaimName.isBlank()
                ? "groups"
                : groupClaimName.strip();
    }
}
