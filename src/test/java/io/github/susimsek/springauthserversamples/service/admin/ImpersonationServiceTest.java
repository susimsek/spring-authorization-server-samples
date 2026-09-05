package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.ImpersonationTicketEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.mapper.AdminImpersonationMapper;
import io.github.susimsek.springauthserversamples.mapper.AdminImpersonationTicketMapper;
import io.github.susimsek.springauthserversamples.repository.ImpersonationTicketRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class ImpersonationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserDetailsService userDetailsService;
    @Mock private ImpersonationTicketRepository ticketRepository;
    @Mock private AdminAuditEventService auditEventService;
    @Mock private AdminImpersonationMapper impersonationMapper;
    @Mock private AdminImpersonationTicketMapper ticketMapper;

    @Test
    void issuesSingleUseTicketForNonAdminUser() {
        UserEntity target = user(7L, "alice");
        when(userRepository.findById(7L)).thenReturn(Optional.of(target));
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(User.withUsername("alice").password("password").roles("USER").build());
        when(ticketMapper.toEntity(
                        anyString(),
                        anyString(),
                        any(UserEntity.class),
                        any(Instant.class),
                        any(Instant.class)))
                .thenAnswer(
                        invocation -> {
                            ImpersonationTicketEntity ticket = new ImpersonationTicketEntity();
                            ticket.setTicketHash(invocation.getArgument(0));
                            ticket.setActorUsername(invocation.getArgument(1));
                            ticket.setTargetUser(invocation.getArgument(2));
                            ticket.setIssuedAt(invocation.getArgument(3));
                            ticket.setExpiresAt(invocation.getArgument(4));
                            return ticket;
                        });
        when(impersonationMapper.toDTO(anyString(), anyString(), anyString()))
                .thenAnswer(
                        invocation ->
                                new io.github.susimsek.springauthserversamples.dto.admin
                                        .AdminImpersonationDTO(
                                        invocation.getArgument(0),
                                        invocation.getArgument(1),
                                        invocation.getArgument(2)));

        var result = service().issue(7L, "admin");

        assertThat(result.url()).isEqualTo("/impersonation/accept");
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.ticket()).isNotBlank();
        verify(ticketRepository).save(any(ImpersonationTicketEntity.class));
        verify(auditEventService).record("user.impersonation.started", "user", "7");
    }

    @Test
    void rejectsAdministratorTargetEvenWhenRoleComesFromEffectiveAuthorities() {
        UserEntity target = user(7L, "alice");
        when(userRepository.findById(7L)).thenReturn(Optional.of(target));
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(
                        User.withUsername("alice")
                                .password("password")
                                .authorities("ROLE_ADMIN")
                                .build());

        assertThatThrownBy(() -> service().issue(7L, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Administrators cannot be impersonated");
    }

    private ImpersonationService service() {
        return new ImpersonationService(
                userRepository,
                userDetailsService,
                ticketRepository,
                auditEventService,
                impersonationMapper,
                ticketMapper);
    }

    private static UserEntity user(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("password");
        user.setEnabled(true);
        return user;
    }
}
