package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(name = "AdminGroup", description = "A group and its effective realm-role mapping.")
public record AdminGroupDTO(
        @Schema(example = "1") Long id,
        @Schema(example = "finance-operators") String name,
        @Schema(example = "[\"ROLE_USER_VIEWER\", \"ROLE_CLIENT_VIEWER\"]") Set<String> roles,
        @Schema(example = "3") long userCount) {}
