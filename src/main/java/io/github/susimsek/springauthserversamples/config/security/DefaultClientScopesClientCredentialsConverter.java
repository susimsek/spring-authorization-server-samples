package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.service.admin.ClientScopeSettings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationConverter;

public final class DefaultClientScopesClientCredentialsConverter
        implements AuthenticationConverter {

    private final OAuth2ClientCredentialsAuthenticationConverter delegate =
            new OAuth2ClientCredentialsAuthenticationConverter();

    @Override
    public Authentication convert(HttpServletRequest request) {
        Authentication authentication = delegate.convert(request);
        if (!(authentication instanceof OAuth2ClientCredentialsAuthenticationToken token)
                || !(token.getPrincipal()
                        instanceof OAuth2ClientAuthenticationToken clientPrincipal)
                || clientPrincipal.getRegisteredClient() == null) {
            return authentication;
        }
        var scopes = new LinkedHashSet<>(token.getScopes());
        scopes.addAll(ClientScopeSettings.defaultScopes(clientPrincipal.getRegisteredClient()));
        if (scopes.equals(token.getScopes())) {
            return token;
        }
        return new OAuth2ClientCredentialsAuthenticationToken(
                clientPrincipal, scopes, token.getAdditionalParameters());
    }
}
