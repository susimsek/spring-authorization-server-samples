package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.service.admin.ClientScopeSettings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationConverter;

public final class DefaultClientScopesAuthorizationRequestConverter
        implements AuthenticationConverter {

    private final OAuth2AuthorizationCodeRequestAuthenticationConverter delegate =
            new OAuth2AuthorizationCodeRequestAuthenticationConverter();
    private final RegisteredClientRepository registeredClientRepository;

    public DefaultClientScopesAuthorizationRequestConverter(
            RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        Authentication authentication = delegate.convert(request);
        if (!(authentication instanceof OAuth2AuthorizationCodeRequestAuthenticationToken token)) {
            return authentication;
        }
        var registeredClient = registeredClientRepository.findByClientId(token.getClientId());
        if (registeredClient == null) {
            return token;
        }
        var scopes = new LinkedHashSet<>(token.getScopes());
        scopes.addAll(ClientScopeSettings.defaultScopes(registeredClient));
        if (scopes.equals(token.getScopes())) {
            return token;
        }
        return new OAuth2AuthorizationCodeRequestAuthenticationToken(
                token.getAuthorizationUri(),
                token.getClientId(),
                (Authentication) token.getPrincipal(),
                token.getRedirectUri(),
                token.getState(),
                scopes,
                token.getAdditionalParameters());
    }
}
