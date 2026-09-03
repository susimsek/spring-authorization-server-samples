package io.github.susimsek.springauthserversamples.dto.admin;

import jakarta.validation.constraints.NotNull;

public record AdminRoleUserRequestDTO(@NotNull Long userId) {}
