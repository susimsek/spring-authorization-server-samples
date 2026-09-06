package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(name = "AdminGroup", description = "A group and its direct and effective role mappings.")
public record AdminGroupDTO(
        @Schema(
                        description = "Internal group identifier.",
                        example = "1",
                        format = "int64",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Long id,
        @Schema(
                        description = "Group name.",
                        example = "finance-operators",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String name,
        @Schema(
                        description = "Full hierarchical group path.",
                        example = "Finance / finance-operators",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String path,
        @Schema(
                        description = "Parent group identifier, if nested.",
                        example = "2",
                        format = "int64",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                Long parentId,
        @Schema(
                        description = "Realm roles assigned directly to the group.",
                        example = "[\"ROLE_USER_VIEWER\", \"ROLE_CLIENT_VIEWER\"]",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Set<String> roles,
        @Schema(
                        description = "All roles effective for the group, including parent groups.",
                        example = "[\"ROLE_USER_VIEWER\", \"ROLE_CLIENT_VIEWER\"]",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Set<String> effectiveRoles,
        @Schema(
                        description = "Number of users in the group.",
                        example = "3",
                        format = "int64",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                long userCount) {

    public AdminGroupDTO(
            Long id, String name, String path, Long parentId, Set<String> roles, long userCount) {
        this(id, name, path, parentId, roles, roles, userCount);
    }
}
