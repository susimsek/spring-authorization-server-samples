package io.github.susimsek.springauthserversamples.web.admin;

import jakarta.validation.constraints.NotNull;

public record AdminUserEnabledRequest(
        @NotNull(message = "{app.api.problem.violation.required}") Boolean enabled) {}
