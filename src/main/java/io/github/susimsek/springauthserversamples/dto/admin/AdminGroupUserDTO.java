package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminGroupUser", description = "A user returned from group membership endpoints.")
public record AdminGroupUserDTO(
        @Schema(example = "2") Long id,
        @Schema(example = "user") String username,
        @Schema(example = "true") boolean enabled) {}
