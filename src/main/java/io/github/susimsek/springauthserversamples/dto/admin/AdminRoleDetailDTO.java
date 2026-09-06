package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

@Schema(name = "AdminRoleDetail", description = "Role details and its paged user membership.")
public record AdminRoleDetailDTO(
        @Schema(
                        description = "Role name.",
                        example = "ROLE_USER_VIEWER",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String name,
        @Schema(
                        description = "Human-readable role description.",
                        example = "Allows viewing user accounts.",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String description,
        @Schema(
                        description = "Number of users assigned to the role.",
                        example = "3",
                        format = "int64",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                long userCount,
        @Schema(
                        description = "Whether the role is protected and cannot be deleted.",
                        example = "false",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                boolean protectedRole,
        @Schema(
                        description = "Users assigned to this role.",
                        example = "{\"content\":[],\"totalElements\":0,\"totalPages\":0}",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Page<AdminRoleUserDTO> users) {

    public AdminRoleDetailDTO(
            String name, long userCount, boolean protectedRole, Page<AdminRoleUserDTO> users) {
        this(name, null, userCount, protectedRole, users);
    }
}
