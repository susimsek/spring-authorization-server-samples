package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminSessionServiceTest {
    @Mock private AdminUserService adminUserService;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private AuthorizationRepository authorizationRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AdminAuditEventService adminAuditEventService;

    @Test
    void returnsSessionsWithAuthorizationCounts() {
        UserSessionEntity aliceSession = session("alice-session", "alice", 1_000L);
        UserSessionEntity anonymousSession = session("anonymous-session", null, 2_000L);
        Pageable pageable = Pageable.unpaged();
        when(userSessionRepository.findActiveSessions(anyLong(), eq("alice"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(aliceSession, anonymousSession)));
        when(authorizationRepository.countBySessionIdIn(
                        List.of("alice-session", "anonymous-session")))
                .thenReturn(List.of(authorizationCount("alice-session", 3L)));

        List<AdminSessionService.SessionView> result =
                service().sessions("  alice  ", pageable).getContent();

        assertThat(result)
                .containsExactly(
                        new AdminSessionService.SessionView(
                                "alice-session",
                                "alice",
                                Instant.ofEpochMilli(1_000L),
                                Instant.ofEpochMilli(1_100L),
                                Instant.ofEpochMilli(1_200L),
                                3L,
                                false),
                        new AdminSessionService.SessionView(
                                "anonymous-session",
                                null,
                                Instant.ofEpochMilli(2_000L),
                                Instant.ofEpochMilli(2_100L),
                                Instant.ofEpochMilli(2_200L),
                                0L,
                                false));
        verify(userSessionRepository).findActiveSessions(anyLong(), eq("alice"), eq(pageable));
        verify(authorizationRepository)
                .countBySessionIdIn(List.of("alice-session", "anonymous-session"));
    }

    @Test
    void returnsEmptySessionsWithoutLoadingAuthorizationCounts() {
        Pageable pageable = Pageable.unpaged();
        when(userSessionRepository.findActiveSessions(anyLong(), eq(""), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service().sessions(null, pageable).getContent()).isEmpty();
        verify(userSessionRepository).findActiveSessions(anyLong(), eq(""), eq(pageable));
        verifyNoInteractions(authorizationRepository);
    }

    @Test
    void deletesSessionAndItsAuthorizations() {
        UserSessionEntity session = new UserSessionEntity();
        session.setSessionId("session-id");
        session.setPrincipalName("user");
        when(userSessionRepository.findBySessionId("session-id")).thenReturn(Optional.of(session));

        service().deleteSession("session-id", "admin");

        verify(adminUserService).assertCanManageUsername("user", "admin");
        verify(userSessionRepository).deleteBySessionId("session-id");
        verify(authorizationRepository).deleteBySessionId("session-id");
        verify(adminAuditEventService).record("session.deleted", "session", "session-id");
    }

    @Test
    void rejectsUnknownSessions() {
        when(userSessionRepository.findBySessionId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().deleteSession("missing", "admin"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Session not found");

        verify(userSessionRepository).findBySessionId("missing");
        verifyNoInteractions(adminUserService, authorizationRepository, adminAuditEventService);
    }

    @Test
    void deletesAllUserSessionsAndAuthorizations() {
        service().deleteUserSessions("user", "admin");

        verify(adminUserService).assertCanManageUsername("user", "admin");
        verify(userSessionRepository).deleteByPrincipalName("user");
        verify(authorizationRepository).deleteByPrincipalName("user");
        verify(adminAuditEventService).record("user.sessions.deleted", "user", "user");
    }

    private static UserSessionEntity session(
            String sessionId, String principalName, long creationTime) {
        UserSessionEntity session = new UserSessionEntity();
        session.setSessionId(sessionId);
        session.setPrincipalName(principalName);
        session.setCreationTime(creationTime);
        session.setLastAccessTime(creationTime + 100L);
        session.setExpiryTime(creationTime + 200L);
        return session;
    }

    private static AuthorizationRepository.SessionAuthorizationCount authorizationCount(
            String sessionId, long authorizationCount) {
        return new AuthorizationRepository.SessionAuthorizationCount() {
            @Override
            public String getSessionId() {
                return sessionId;
            }

            @Override
            public long getAuthorizationCount() {
                return authorizationCount;
            }
        };
    }

    private AdminSessionService service() {
        return new AdminSessionService(
                adminUserService,
                userSessionRepository,
                authorizationRepository,
                clientRepository,
                adminAuditEventService);
    }
}
