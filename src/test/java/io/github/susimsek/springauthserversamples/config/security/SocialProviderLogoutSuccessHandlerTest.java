package io.github.susimsek.springauthserversamples.config.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.service.SocialLoginService;
import io.github.susimsek.springauthserversamples.service.SocialProviderSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationToken;

class SocialProviderLogoutSuccessHandlerTest {

    @Test
    void startsMicrosoftLogoutWithTheRegisteredPostLogoutUri() throws Exception {
        SocialProviderSettingsService settings = mock(SocialProviderSettingsService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Authentication principal =
                UsernamePasswordAuthenticationToken.authenticated("ada", null, java.util.List.of());
        OidcLogoutAuthenticationToken logout =
                new OidcLogoutAuthenticationToken(
                        "id-token-hint",
                        principal,
                        "account-console",
                        null,
                        "http://localhost:9090/account/",
                        null);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SocialLoginService.SOCIAL_LOGIN_PROVIDER))
                .thenReturn("microsoft");
        when(settings.provider("microsoft"))
                .thenReturn(
                        new SocialProviderSettingsService.ProviderCredentials(
                                "microsoft", "client", "secret"));

        new SocialProviderLogoutSuccessHandler(settings)
                .onAuthenticationSuccess(request, response, logout);

        verify(response)
                .sendRedirect(
                        "https://login.microsoftonline.com/common/oauth2/v2.0/logout?post_logout_redirect_uri=http%3A%2F%2Flocalhost%3A9090%2Faccount%2F");
        verify(session).removeAttribute(SocialLoginService.SOCIAL_LOGIN_PROVIDER);
    }
}
