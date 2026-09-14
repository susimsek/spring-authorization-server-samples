package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(
        name = "AdminGroupPermissionsRequest",
        description = "Complete group-scoped permission assignments.")
public record AdminGroupPermissionsRequestDTO(
        @NotNull
                @Size(max = 200)
                @Valid
                @Schema(
                        description = "Complete permission assignment list.",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                List<AdminGroupPermissionRequestDTO> permissions) {}
