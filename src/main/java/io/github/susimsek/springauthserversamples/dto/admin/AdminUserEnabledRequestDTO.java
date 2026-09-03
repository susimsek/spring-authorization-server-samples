package io.github.susimsek.springauthserversamples.dto.admin;

import jakarta.validation.constraints.NotNull;

public record AdminUserEnabledRequestDTO(
        @NotNull(message = "{app.api.problem.violation.required}") Boolean enabled) {}
