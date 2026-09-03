package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import io.github.susimsek.springauthserversamples.domain.RegisteredClientEntity;
import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminAuthorizationDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSessionDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSessionDetailDTO;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import io.github.susimsek.springauthserversamples.service.SessionInvalidationService;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
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
    @Mock private SessionInvalidationService sessionInvalidationService;

    @Test
    void returnsSessionsWithAuthorizationCounts() {
        UserSessionEntity aliceSession = session("alice-session", "alice", 1_000L);
        UserSessionEntity anonymousSession = session("anonymous-session", null, 2_000L);
        Pageable pageable = Pageable.unpaged();
        when(userSessionRepository.findSessions(anyLong(), eq("alice"), eq("active"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(aliceSession, anonymousSession)));
        when(authorizationRepository.countBySessionIdIn(
                        List.of("alice-session", "anonymous-session")))
                .thenReturn(List.of(authorizationCount("alice-session", 3L)));

        List<AdminSessionDTO> result =
                service().sessions("  alice  ", "", "active", pageable).getContent();

        assertThat(result)
                .containsExactly(
                        new AdminSessionDTO(
                                "alice-session",
                                "alice",
                                Instant.ofEpochMilli(1_000L),
                                Instant.ofEpochMilli(1_100L),
                                Instant.ofEpochMilli(1_200L),
                                3L,
                                false),
                        new AdminSessionDTO(
                                "anonymous-session",
                                null,
                                Instant.ofEpochMilli(2_000L),
                                Instant.ofEpochMilli(2_100L),
                                Instant.ofEpochMilli(2_200L),
                                0L,
                                false));
        verify(userSessionRepository)
                .findSessions(anyLong(), eq("alice"), eq("active"), eq(pageable));
        verify(authorizationRepository)
                .countBySessionIdIn(List.of("alice-session", "anonymous-session"));
    }

    @Test
    void returnsEmptySessionsWithoutLoadingAuthorizationCounts() {
        Pageable pageable = Pageable.unpaged();
        when(userSessionRepository.findSessions(anyLong(), eq(""), eq("active"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service().sessions(null, "", "active", pageable).getContent()).isEmpty();
        verify(userSessionRepository).findSessions(anyLong(), eq(""), eq("active"), eq(pageable));
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
        verify(sessionInvalidationService).invalidateSession("session-id");
        verify(adminAuditEventService).record("session.deleted", "session", "session-id");
    }

    @Test
    void loadsAuthorizationClientsForSessionDetailInOneBatch() {
        UserSessionEntity session = session("session-id", "user", 1_000L);
        AuthorizationEntity first = authorization("authorization-1", "client-1");
        AuthorizationEntity second = authorization("authorization-2", "client-2");
        RegisteredClientEntity firstClient = client("client-1", "first-client", "First Client");
        RegisteredClientEntity secondClient = client("client-2", "second-client", "Second Client");
        when(userSessionRepository.findBySessionId("session-id")).thenReturn(Optional.of(session));
        when(authorizationRepository.findAllBySessionIdOrderByAccessTokenIssuedAtDesc("session-id"))
                .thenReturn(List.of(first, second));
        when(clientRepository.findAllById(List.of("client-1", "client-2")))
                .thenReturn(List.of(firstClient, secondClient));

        AdminSessionDetailDTO detail = service().session("session-id", "admin");

        assertThat(detail.authorizations())
                .extracting(AdminAuthorizationDTO::clientName)
                .containsExactly("First Client", "Second Client");
        verify(clientRepository).findAllById(List.of("client-1", "client-2"));
        verify(clientRepository, never()).findById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsUnknownSessions() {
        when(userSessionRepository.findBySessionId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().deleteSession("missing", "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Session not found");

        verify(userSessionRepository).findBySessionId("missing");
        verifyNoInteractions(adminUserService, authorizationRepository, adminAuditEventService);
    }

    @Test
    void deletesAllUserSessionsAndAuthorizations() {
        service().deleteUserSessions("user", "admin");

        verify(adminUserService).assertCanManageUsername("user", "admin");
        verify(sessionInvalidationService).invalidatePrincipal("user");
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

    private static AuthorizationEntity authorization(String id, String clientId) {
        AuthorizationEntity authorization = new AuthorizationEntity();
        authorization.setId(id);
        authorization.setRegisteredClientId(clientId);
        authorization.setAuthorizationGrantType("authorization_code");
        return authorization;
    }

    private static RegisteredClientEntity client(String id, String clientId, String clientName) {
        RegisteredClientEntity client = new RegisteredClientEntity();
        client.setId(id);
        client.setClientId(clientId);
        client.setClientName(clientName);
        return client;
    }

    private AdminSessionService service() {
        return new AdminSessionService(
                adminUserService,
                userSessionRepository,
                authorizationRepository,
                clientRepository,
                adminAuditEventService,
                sessionInvalidationService);
    }
}
