package io.github.susimsek.springauthserversamples.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalizedAccessDeniedHandler implements AccessDeniedHandler {

    private final OAuth2ErrorLocalizer errorLocalizer;

    private final OAuth2ErrorHttpMessageConverter errorHttpMessageConverter =
            new OAuth2ErrorHttpMessageConverter();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        OAuth2Error error =
                new OAuth2Error(
                        OAuth2ErrorCodes.ACCESS_DENIED,
                        errorLocalizer.localize(
                                "app.auth.accessDenied",
                                "You do not have permission to access this resource.",
                                request.getLocale()),
                        null);
        errorHttpMessageConverter.write(error, null, new ServletServerHttpResponse(response));
    }
}
