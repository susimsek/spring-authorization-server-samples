package io.github.susimsek.springauthserversamples.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.service.SessionInvalidationService;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.ImpersonationService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;

@ExtendWith(MockitoExtension.class)
class ImpersonationControllerTest {

    @Mock private ImpersonationService impersonationService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private SessionInvalidationService sessionInvalidationService;
    @Mock private AdminAuditEventService auditEventService;
    @Mock private SecurityContextRepository securityContextRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preservesAuthenticationFactorForOidcTokenGeneration() throws Exception {
        UserEntity target = new UserEntity();
        target.setId(2L);
        target.setUsername("user");
        when(impersonationService.consume(anyString(), anyString())).thenReturn(target);
        when(userDetailsService.loadUserByUsername("user"))
                .thenReturn(User.withUsername("user").password("password").roles("USER").build());

        FactorGrantedAuthority factor =
                FactorGrantedAuthority.withAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY)
                        .build();
        Authentication actor =
                UsernamePasswordAuthenticationToken.authenticated(
                        User.withUsername("admin").password("password").roles("ADMIN").build(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), factor));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession();
        request.setCookies(new Cookie("IMPERSONATION_TICKET", "ticket"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller().accept(actor, request, response);

        ArgumentCaptor<SecurityContext> context = ArgumentCaptor.forClass(SecurityContext.class);
        verify(securityContextRepository).saveContext(context.capture(), any(), any());
        assertThat(context.getValue().getAuthentication().getAuthorities())
                .anyMatch(factor::equals);
        assertThat(response.getRedirectedUrl()).isEqualTo("/account/?impersonated=1");
    }

    private ImpersonationController controller() {
        return new ImpersonationController(
                impersonationService,
                userDetailsService,
                sessionInvalidationService,
                auditEventService,
                securityContextRepository);
    }
}
