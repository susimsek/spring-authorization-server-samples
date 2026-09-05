package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminRole", description = "Realm role name.")
public record AdminRoleDTO(
        @Schema(
                        description = "Role name.",
                        example = "ROLE_USER_VIEWER",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String name) {}
