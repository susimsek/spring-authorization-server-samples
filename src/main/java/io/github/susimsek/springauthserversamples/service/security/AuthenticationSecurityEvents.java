package io.github.susimsek.springauthserversamples.service.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class AuthenticationSecurityEvents {

    private final AccountLockService accountLockService;
    private final LoginRateLimitService loginRateLimitService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = username(event.getAuthentication());
        accountLockService.recordSuccess(username);
        loginRateLimitService.clear(username, remoteAddress());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        accountLockService.recordFailure(username(event.getAuthentication()), remoteAddress());
    }

    private static String username(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();
        return principal instanceof String value
                ? value
                : authentication == null ? null : authentication.getName();
    }

    private static String remoteAddress() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return "unknown";
    }
}
