package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.service.account.MfaService;
import io.github.susimsek.springauthserversamples.service.requiredaction.RequiredActionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Requires a verified TOTP factor before an OAuth authorization request can proceed. */
@Component
@RequiredArgsConstructor
public class MfaAuthorizationFilter extends OncePerRequestFilter {

    public static final String MFA_VERIFIED = "MFA_TOTP_VERIFIED";
    private final MfaService mfaService;
    private final RequiredActionService requiredActionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ("/oauth2/authorize".equals(request.getRequestURI())
                && authentication != null
                && authentication.isAuthenticated()
                && !authentication.getClass().getName().contains("Anonymous")
                && requiredActionService.pending(authentication.getName()).isEmpty()) {
            var status = mfaService.status(authentication.getName());
            if (!status.available()
                    || !status.enabled()
                    || Boolean.TRUE.equals(request.getSession().getAttribute(MFA_VERIFIED))) {
                filterChain.doFilter(request, response);
                return;
            }
            String current = request.getRequestURI();
            if (request.getQueryString() != null) {
                current += "?" + request.getQueryString();
            }
            response.sendRedirect(
                    "/mfa?return_to=" + URLEncoder.encode(current, StandardCharsets.UTF_8));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
