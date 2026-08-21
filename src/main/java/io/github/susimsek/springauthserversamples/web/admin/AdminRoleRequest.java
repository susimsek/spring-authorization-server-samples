package io.github.susimsek.springauthserversamples.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminRoleRequest(
        @NotBlank(message = "{admin.validation.required}")
                @Pattern(regexp = "ROLE_[A-Z0-9_]+", message = "{admin.validation.role_format}")
                String name) {}
