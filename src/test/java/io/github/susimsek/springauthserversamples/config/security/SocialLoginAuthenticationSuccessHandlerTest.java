package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.service.SocialLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;

class SocialLoginAuthenticationSuccessHandlerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addsAuthenticationFactorForOidcTokenGeneration() throws Exception {
        SocialLoginService socialLoginService = mock(SocialLoginService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        SecurityContextRepository securityContextRepository = mock(SecurityContextRepository.class);
        OAuth2AuthenticationToken oauth2Authentication = mock(OAuth2AuthenticationToken.class);
        UserDetails user =
                User.withUsername("social_user").password("encoded").roles("USER").build();
        when(socialLoginService.findOrCreate(oauth2Authentication)).thenReturn("social_user");
        when(userDetailsService.loadUserByUsername("social_user")).thenReturn(user);

        SocialLoginAuthenticationSuccessHandler handler =
                new SocialLoginAuthenticationSuccessHandler(
                        socialLoginService, userDetailsService, securityContextRepository);
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, oauth2Authentication);

        Authentication localAuthentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(localAuthentication.getAuthorities())
                .anyMatch(
                        authority ->
                                authority instanceof FactorGrantedAuthority factor
                                        && factor.getAuthority()
                                                .equals(
                                                        FactorGrantedAuthority
                                                                .AUTHORIZATION_CODE_AUTHORITY)
                                        && factor.getIssuedAt() != null);
        verify(securityContextRepository).saveContext(any(), any(), any());
    }
}
