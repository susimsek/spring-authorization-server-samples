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

    private static final String ADMIN_CONSOLE_CLIENT_ID = "admin-console";

    @Override
    public @Nullable Authentication convert(HttpServletRequest request) {
        if (!(AuthorizationGrantType.REFRESH_TOKEN
                                .getValue()
                                .equals(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                        || request.getRequestURI().endsWith("/oauth2/revoke"))
                || !ADMIN_CONSOLE_CLIENT_ID.equals(
                        request.getParameter(OAuth2ParameterNames.CLIENT_ID))) {
            return null;
        }

        return new OAuth2ClientAuthenticationToken(
                ADMIN_CONSOLE_CLIENT_ID, ClientAuthenticationMethod.NONE, null, null);
    }
}
