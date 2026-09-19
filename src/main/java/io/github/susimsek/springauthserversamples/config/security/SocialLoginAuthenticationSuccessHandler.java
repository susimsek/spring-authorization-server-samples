package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.service.SocialAccountLinkRequiredException;
import io.github.susimsek.springauthserversamples.service.SocialLoginService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.context.SecurityContextRepository;

@RequiredArgsConstructor
public class SocialLoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final io.github.susimsek.springauthserversamples.service.SocialLoginService
            socialLoginService;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final AuthenticationFailureHandler failureHandler =
            new SimpleUrlAuthenticationFailureHandler("/login?error");
    private final AuthenticationFailureHandler accountLinkFailureHandler =
            new SimpleUrlAuthenticationFailureHandler("/login?account_link_required");
    private final SavedRequestAwareAuthenticationSuccessHandler delegate = delegate();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication)) {
                throw new IllegalStateException("Social login requires an OAuth2 authentication");
            }
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            Object linkTarget =
                    session == null
                            ? null
                            : session.getAttribute(SocialLoginService.PENDING_SOCIAL_LINK_TARGET);
            String username;
            if (linkTarget instanceof java.util.Map<?, ?> target) {
                username =
                        socialLoginService.linkExisting(
                                value(target, "username"),
                                value(target, "provider"),
                                oauth2Authentication);
                session.removeAttribute(SocialLoginService.PENDING_SOCIAL_LINK_TARGET);
            } else {
                username = socialLoginService.findOrCreate(oauth2Authentication);
            }
            UserDetails user = userDetailsService.loadUserByUsername(username);
            Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());
            authorities.add(
                    FactorGrantedAuthority.withAuthority(
                                    FactorGrantedAuthority.AUTHORIZATION_CODE_AUTHORITY)
                            .issuedAt(Instant.now())
                            .build());
            Authentication localAuthentication =
                    UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(localAuthentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            if (linkTarget instanceof java.util.Map<?, ?>) {
                response.sendRedirect("/account/security?social_linked=1");
            } else {
                delegate.onAuthenticationSuccess(request, response, localAuthentication);
            }
        } catch (RuntimeException exception) {
            if (exception instanceof SocialAccountLinkRequiredException linkRequired) {
                request.getSession(true)
                        .setAttribute(
                                SocialLoginService.PENDING_SOCIAL_LINK, linkRequired.pendingLink());
                accountLinkFailureHandler.onAuthenticationFailure(request, response, linkRequired);
                return;
            }
            failureHandler.onAuthenticationFailure(
                    request,
                    response,
                    exception instanceof AuthenticationException authenticationException
                            ? authenticationException
                            : new org.springframework.security.authentication
                                    .AuthenticationServiceException(
                                    "Social login could not be completed", exception));
        }
    }

    private static String value(java.util.Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static SavedRequestAwareAuthenticationSuccessHandler delegate() {
        SavedRequestAwareAuthenticationSuccessHandler handler =
                new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl("/admin");
        return handler;
    }
}
