package io.github.susimsek.springauthserversamples.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalizedAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final OAuth2ErrorLocalizer errorLocalizer;

    private final OAuth2ErrorHttpMessageConverter errorHttpMessageConverter =
            new OAuth2ErrorHttpMessageConverter();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException)
            throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        OAuth2Error error =
                new OAuth2Error(
                        "unauthorized",
                        errorLocalizer.localize(
                                "app.auth.unauthorized",
                                "Authentication is required.",
                                request.getLocale()),
                        null);
        errorHttpMessageConverter.write(error, null, new ServletServerHttpResponse(response));
    }
}
