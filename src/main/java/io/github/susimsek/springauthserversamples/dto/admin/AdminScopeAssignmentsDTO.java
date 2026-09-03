package io.github.susimsek.springauthserversamples.dto.admin;

import java.util.List;
import java.util.Set;

public record AdminScopeAssignmentsDTO(
        Set<String> defaultScopes,
        Set<String> optionalScopes,
        List<AdminClientScopeDTO> availableScopes) {}
