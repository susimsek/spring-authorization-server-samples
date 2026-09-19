package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.service.SocialLoginService;
import io.github.susimsek.springauthserversamples.service.SocialProviderSettingsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.oidc.web.authentication.OidcLogoutAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/** Starts an upstream broker logout when the provider exposes a browser logout endpoint. */
@RequiredArgsConstructor
public class SocialProviderLogoutSuccessHandler implements AuthenticationSuccessHandler {

    private final SocialProviderSettingsService providerSettingsService;
    private final OidcLogoutAuthenticationSuccessHandler delegate =
            new OidcLogoutAuthenticationSuccessHandler();
    private final SecurityContextLogoutHandler securityContextLogoutHandler =
            new SecurityContextLogoutHandler();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (!(authentication instanceof OidcLogoutAuthenticationToken logout)) {
            delegate.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        String provider =
                session == null
                        ? null
                        : (String) session.getAttribute(SocialLoginService.SOCIAL_LOGIN_PROVIDER);
        SocialProviderSettingsService.ProviderCredentials credentials =
                providerSettingsService.provider(provider);
        String upstreamLogout =
                credentials == null
                        ? null
                        : upstreamLogout(
                                credentials.registrationId(), logout.getPostLogoutRedirectUri());
        if (upstreamLogout == null) {
            delegate.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        if (logout.isPrincipalAuthenticated()) {
            if (session != null) {
                session.removeAttribute(SocialLoginService.SOCIAL_LOGIN_PROVIDER);
            }
            securityContextLogoutHandler.logout(
                    request, response, (Authentication) logout.getPrincipal());
        }
        response.sendRedirect(upstreamLogout);
    }

    private static String upstreamLogout(String provider, String redirectUri) {
        String redirect = redirectUri == null || redirectUri.isBlank() ? "/" : redirectUri;
        String encoded = URLEncoder.encode(redirect, StandardCharsets.UTF_8);
        return switch (provider) {
            case "google" -> "https://accounts.google.com/Logout?continue=" + encoded;
            case "microsoft" ->
                    "https://login.microsoftonline.com/common/oauth2/v2.0/logout?post_logout_redirect_uri="
                            + encoded;
            default -> null;
        };
    }
}
