package io.github.susimsek.springauthserversamples.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminClientScopeRequestDTO(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 200) String displayName,
        @Size(max = 500) String description) {}
