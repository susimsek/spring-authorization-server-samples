package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.dto.account.MfaStatusDTO;
import io.github.susimsek.springauthserversamples.service.account.MfaService;
import io.github.susimsek.springauthserversamples.service.requiredaction.RequiredActionService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class MfaAuthorizationFilterTest {

    private final MfaService mfaService = mock(MfaService.class);
    private final RequiredActionService requiredActionService = mock(RequiredActionService.class);
    private final jakarta.servlet.FilterChain filterChain = mock(jakarta.servlet.FilterChain.class);
    private final MfaAuthorizationFilter filter =
            new MfaAuthorizationFilter(mfaService, requiredActionService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void redirectsAnEnabledUserWhoseCurrentSessionIsNotVerified() throws Exception {
        authenticate("alice");
        when(requiredActionService.pending("alice")).thenReturn(List.of());
        when(mfaService.status("alice")).thenReturn(status(true, true));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        request.setQueryString("client_id=account-console&state=request-state");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getRedirectedUrl())
                .isEqualTo(
                        "/mfa?return_to=%2Foauth2%2Fauthorize%3Fclient_id%3Daccount-console%26state%3Drequest-state");
        verifyNoInteractions(filterChain);
    }

    @Test
    void permitsAuthorizationAfterTheCurrentSessionHasVerifiedMfa() throws Exception {
        authenticate("alice");
        when(requiredActionService.pending("alice")).thenReturn(List.of());
        when(mfaService.status("alice")).thenReturn(status(true, true));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        request.getSession().setAttribute(MfaAuthorizationFilter.MFA_VERIFIED, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void permitsAuthorizationWhenMfaIsUnavailableOrNotEnabledForTheUser() throws Exception {
        authenticate("alice");
        when(requiredActionService.pending("alice")).thenReturn(List.of());
        when(mfaService.status("alice")).thenReturn(status(false, true), status(true, false));
        MockHttpServletRequest firstRequest =
                new MockHttpServletRequest("GET", "/oauth2/authorize");
        MockHttpServletRequest secondRequest =
                new MockHttpServletRequest("GET", "/oauth2/authorize");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        filter.doFilter(firstRequest, firstResponse, filterChain);
        filter.doFilter(secondRequest, secondResponse, filterChain);

        verify(filterChain).doFilter(firstRequest, firstResponse);
        verify(filterChain).doFilter(secondRequest, secondResponse);
    }

    private static MfaStatusDTO status(boolean available, boolean enabled) {
        return new MfaStatusDTO(enabled, available, false, "Issuer", "SHA1", 6, 30);
    }

    private static void authenticate(String username) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                username,
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
