package io.github.susimsek.springauthserversamples.web.admin;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AdminClientScopeAssignmentRequest(
        @NotNull Set<String> defaultScopes, @NotNull Set<String> optionalScopes) {}
