package io.github.susimsek.springauthserversamples.web.admin;

import jakarta.validation.constraints.NotNull;

record AdminRoleUserRequest(@NotNull Long userId) {}
