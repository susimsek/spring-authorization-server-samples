package io.github.susimsek.springauthserversamples.config.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

final class AdminConsoleRefreshClientAuthenticationProvider implements AuthenticationProvider {

    private static final String ADMIN_CONSOLE_CLIENT_ID = "admin-console";

    private final RegisteredClientRepository registeredClientRepository;

    AdminConsoleRefreshClientAuthenticationProvider(
            RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public @Nullable Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        OAuth2ClientAuthenticationToken clientAuthentication =
                (OAuth2ClientAuthenticationToken) authentication;
        if (!ClientAuthenticationMethod.NONE.equals(
                        clientAuthentication.getClientAuthenticationMethod())
                || !ADMIN_CONSOLE_CLIENT_ID.equals(clientAuthentication.getPrincipal())) {
            return null;
        }

        RegisteredClient registeredClient =
                registeredClientRepository.findByClientId(ADMIN_CONSOLE_CLIENT_ID);
        if (registeredClient == null
                || !registeredClient
                        .getClientAuthenticationMethods()
                        .contains(ClientAuthenticationMethod.NONE)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
        }

        return new OAuth2ClientAuthenticationToken(
                registeredClient, ClientAuthenticationMethod.NONE, null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
