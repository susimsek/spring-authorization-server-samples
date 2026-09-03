package io.github.susimsek.springauthserversamples.dto.admin;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AdminClientScopeAssignmentRequestDTO(
        @NotNull Set<String> defaultScopes, @NotNull Set<String> optionalScopes) {}
