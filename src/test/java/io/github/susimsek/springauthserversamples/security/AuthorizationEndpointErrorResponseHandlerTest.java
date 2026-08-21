package io.github.susimsek.springauthserversamples.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.web.servlet.LocaleResolver;

class AuthorizationEndpointErrorResponseHandlerTest {

    private final LocaleResolver localeResolver = mock(LocaleResolver.class);
    private final AuthorizationEndpointErrorResponseHandler handler =
            new AuthorizationEndpointErrorResponseHandler(localeResolver);

    @Test
    void redirectsBackToClientWhenRedirectUriExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2Error error =
                new OAuth2Error(
                        "invalid_scope", "scope needs space and slash /", "https://issuer/error");
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationRequest =
                new OAuth2AuthorizationCodeRequestAuthenticationToken(
                        "https://issuer.example/oauth2/authorize",
                        "client-id",
                        new TestingAuthenticationToken("user", "pw"),
                        "https://client.example/callback",
                        "state-123",
                        Set.of("openid"),
                        Map.of());

        handler.onAuthenticationFailure(
                request,
                response,
                new OAuth2AuthorizationCodeRequestAuthenticationException(
                        error, authorizationRequest));

        assertThat(response.getRedirectedUrl())
                .isEqualTo(
                        "https://client.example/callback?error=invalid_scope"
                                + "&error_description=scope%20needs%20space%20and%20slash%20%2F"
                                + "&error_uri=https%3A%2F%2Fissuer%2Ferror&state=state-123");
    }

    @Test
    void redirectsToLocalizedLocalErrorPageWhenRedirectUriMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(localeResolver.resolveLocale(request))
                .thenReturn(java.util.Locale.forLanguageTag("tr"));
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationRequest =
                new OAuth2AuthorizationCodeRequestAuthenticationToken(
                        "https://issuer.example/oauth2/authorize",
                        "client-id",
                        new TestingAuthenticationToken("user", "pw"),
                        null,
                        "state-123",
                        Set.of("openid"),
                        Map.of());

        handler.onAuthenticationFailure(
                request,
                response,
                new OAuth2AuthorizationCodeRequestAuthenticationException(
                        new OAuth2Error("invalid_request"), authorizationRequest));

        assertThat(response.getRedirectedUrl()).isEqualTo("/tr/error?type=invalid_request");
    }

    @Test
    void redirectsToLocalizedLocalErrorPageWhenAuthorizationRequestMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(localeResolver.resolveLocale(request)).thenReturn(java.util.Locale.ENGLISH);

        handler.onAuthenticationFailure(
                request,
                response,
                new OAuth2AuthorizationCodeRequestAuthenticationException(
                        new OAuth2Error("server_error"), null));

        assertThat(response.getRedirectedUrl()).isEqualTo("/en/error?type=server_error");
    }
}
