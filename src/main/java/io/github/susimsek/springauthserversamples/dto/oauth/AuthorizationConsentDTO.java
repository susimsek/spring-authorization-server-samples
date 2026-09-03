package io.github.susimsek.springauthserversamples.dto.oauth;

import java.util.Set;

public record AuthorizationConsentDTO(
        String clientId,
        String state,
        Set<String> scopes,
        Set<String> previouslyApprovedScopes,
        String principalName,
        String userCode,
        String requestUri) {}
