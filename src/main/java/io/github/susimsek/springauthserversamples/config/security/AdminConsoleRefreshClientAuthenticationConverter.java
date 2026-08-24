package io.github.susimsek.springauthserversamples.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;

final class AdminConsoleRefreshClientAuthenticationConverter implements AuthenticationConverter {

    private static final java.util.Set<String> CONSOLE_CLIENT_IDS =
            java.util.Set.of("admin-console", "account-console");

    @Override
    public @Nullable Authentication convert(HttpServletRequest request) {
        if (!(AuthorizationGrantType.REFRESH_TOKEN
                        .getValue()
                        .equals(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                || request.getRequestURI().endsWith("/oauth2/revoke"))) {
            return null;
        }

        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
        if (clientId == null || !CONSOLE_CLIENT_IDS.contains(clientId)) {
            return null;
        }
        return new OAuth2ClientAuthenticationToken(
                clientId, ClientAuthenticationMethod.NONE, null, null);
    }
}
