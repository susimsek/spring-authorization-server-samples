package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.service.LoginSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.stereotype.Component;

/** Enables remember-me only while the application setting is enabled. */
@Component
@RequiredArgsConstructor
public class DynamicRememberMeServices implements RememberMeServices {

    private final LoginSettingsService loginSettingsService;
    private final UserDetailsService userDetailsService;
    private volatile TokenBasedRememberMeServices delegate;

    private TokenBasedRememberMeServices delegate() {
        TokenBasedRememberMeServices current = delegate;
        if (current == null) {
            current =
                    new TokenBasedRememberMeServices(
                            "spring-authorization-server-samples", userDetailsService);
            current.setTokenValiditySeconds(14 * 24 * 60 * 60);
            delegate = current;
        }
        return current;
    }

    @Override
    public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
        return loginSettingsService.isRememberMeEnabled()
                ? delegate().autoLogin(request, response)
                : null;
    }

    @Override
    public void loginFail(HttpServletRequest request, HttpServletResponse response) {
        if (loginSettingsService.isRememberMeEnabled()) {
            delegate().loginFail(request, response);
        }
    }

    @Override
    public void loginSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication successfulAuthentication) {
        if (loginSettingsService.isRememberMeEnabled()) {
            delegate().loginSuccess(request, response, successfulAuthentication);
        }
    }
}
