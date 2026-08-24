package io.github.susimsek.springauthserversamples.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminClientScopeRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 200) String displayName,
        @Size(max = 500) String description) {}
