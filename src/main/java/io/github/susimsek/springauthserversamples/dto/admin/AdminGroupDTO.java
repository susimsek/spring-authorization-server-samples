package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(name = "AdminGroup", description = "A group and its effective realm-role mapping.")
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
                        description = "Realm roles effectively mapped to the group.",
                        example = "[\"ROLE_USER_VIEWER\", \"ROLE_CLIENT_VIEWER\"]",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Set<String> roles,
        @Schema(
                        description = "Number of users in the group.",
                        example = "3",
                        format = "int64",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                long userCount) {}
