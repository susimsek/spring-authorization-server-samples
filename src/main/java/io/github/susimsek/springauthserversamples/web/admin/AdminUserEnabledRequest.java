package io.github.susimsek.springauthserversamples.web.admin;

import jakarta.validation.constraints.NotNull;

public record AdminUserEnabledRequest(
        @NotNull(message = "{admin.validation.required}") Boolean enabled) {}
