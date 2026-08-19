package io.github.susimsek.springauthserversamples.web;

import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorizationConsentController {

    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationConsentService authorizationConsentService;

    public AuthorizationConsentController(
            RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationConsentService authorizationConsentService) {
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationConsentService = authorizationConsentService;
    }

    @GetMapping("/api/authorization/consent")
    ConsentView consent(
            Principal principal,
            @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
            @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
            @RequestParam(OAuth2ParameterNames.STATE) String state,
            @RequestParam(name = OAuth2ParameterNames.USER_CODE, required = false)
                    String userCode) {

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw new IllegalArgumentException("Invalid client");
        }

        Set<String> requestedScopes =
                new LinkedHashSet<>(
                        Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
        if (!registeredClient.getScopes().containsAll(requestedScopes)) {
            throw new IllegalArgumentException("Invalid scope");
        }

        OAuth2AuthorizationConsent currentConsent =
                authorizationConsentService.findById(registeredClient.getId(), principal.getName());

        Set<String> previouslyApprovedScopes =
                currentConsent != null ? currentConsent.getScopes() : Set.of();

        Set<String> scopesToApprove = new LinkedHashSet<>();
        Set<String> approvedScopes = new LinkedHashSet<>();

        for (String requestedScope : requestedScopes) {
            if (OidcScopes.OPENID.equals(requestedScope)) {
                continue;
            }
            if (previouslyApprovedScopes.contains(requestedScope)) {
                approvedScopes.add(requestedScope);
            } else {
                scopesToApprove.add(requestedScope);
            }
        }

        String requestUri =
                StringUtils.hasText(userCode) ? "/oauth2/device_verification" : "/oauth2/authorize";

        return new ConsentView(
                clientId,
                state,
                scopesToApprove,
                approvedScopes,
                principal.getName(),
                userCode,
                requestUri);
    }

    record ConsentView(
            String clientId,
            String state,
            Set<String> scopes,
            Set<String> previouslyApprovedScopes,
            String principalName,
            String userCode,
            String requestUri) {}
}
