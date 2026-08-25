package io.github.susimsek.springauthserversamples.service.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import io.github.susimsek.springauthserversamples.domain.RegisteredClientEntity;
import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import io.github.susimsek.springauthserversamples.mapper.AccountProfileMapper;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationConsentRepository;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private AuthorizationConsentRepository authorizationConsentRepository;
    @Mock private AuthorizationRepository authorizationRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AuthorizationServerMapperSupport mapperSupport;
    @Mock private AccountProfileMapper accountProfileMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AdminAuditEventService auditEventService;

    @Test
    void loadsSessionAuthorizationClientsInBatches() {
        Pageable pageable = Pageable.unpaged();
        UserSessionEntity firstSession = session("session-1");
        UserSessionEntity secondSession = session("session-2");
        when(userSessionRepository.findActiveSessionsByPrincipalName(
                        anyLong(), eq("alice"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(firstSession, secondSession)));
        when(authorizationRepository.findAllBySessionIdInOrderByAccessTokenIssuedAtDesc(
                        List.of("session-1", "session-2")))
                .thenReturn(
                        List.of(
                                authorization("authorization-1", "session-1", "client-1"),
                                authorization("authorization-2", "session-1", "client-2"),
                                authorization("authorization-3", "session-2", "client-1")));
        when(clientRepository.findAllById(List.of("client-1", "client-2")))
                .thenReturn(
                        List.of(
                                client("client-1", "First Client"),
                                client("client-2", "Second Client")));

        var result = service().sessions("alice", "session-1", pageable).getContent();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().current()).isTrue();
        assertThat(result.getFirst().clients())
                .extracting(client -> client.clientName())
                .containsExactly("First Client", "Second Client");
        assertThat(result.get(1).clients())
                .extracting(client -> client.clientName())
                .containsExactly("First Client");
        verify(authorizationRepository)
                .findAllBySessionIdInOrderByAccessTokenIssuedAtDesc(
                        List.of("session-1", "session-2"));
        verify(clientRepository).findAllById(List.of("client-1", "client-2"));
    }

    private AccountService service() {
        return new AccountService(
                userRepository,
                userSessionRepository,
                authorizationConsentRepository,
                authorizationRepository,
                clientRepository,
                mapperSupport,
                accountProfileMapper,
                passwordEncoder,
                auditEventService);
    }

    private static UserSessionEntity session(String sessionId) {
        UserSessionEntity session = new UserSessionEntity();
        session.setSessionId(sessionId);
        session.setCreationTime(1_000L);
        session.setLastAccessTime(1_100L);
        session.setExpiryTime(1_200L);
        return session;
    }

    private static AuthorizationEntity authorization(String id, String sessionId, String clientId) {
        AuthorizationEntity authorization = new AuthorizationEntity();
        authorization.setId(id);
        authorization.setSessionId(sessionId);
        authorization.setRegisteredClientId(clientId);
        return authorization;
    }

    private static RegisteredClientEntity client(String id, String clientName) {
        RegisteredClientEntity client = new RegisteredClientEntity();
        client.setId(id);
        client.setClientName(clientName);
        return client;
    }
}
