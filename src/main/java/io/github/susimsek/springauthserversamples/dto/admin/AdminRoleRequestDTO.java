package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "AdminRoleRequest", description = "Realm role to create.")
public record AdminRoleRequestDTO(
        @NotBlank(message = "{app.api.problem.violation.required}")
                @Pattern(
                        regexp = "ROLE_[A-Z0-9_]+",
                        message = "{app.api.problem.violation.role_format}")
                @Size(max = 50)
                @Schema(
                        description = "Role name; must match `ROLE_[A-Z0-9_]+`.",
                        example = "ROLE_REPORT_VIEWER",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String name) {}
