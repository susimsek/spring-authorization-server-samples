package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSessionService {

    private final AdminUserService adminUserService;
    private final UserSessionRepository userSessionRepository;
    private final AuthorizationRepository authorizationRepository;
    private final ClientRepository clientRepository;
    private final AdminAuditEventService adminAuditEventService;

    @Transactional(readOnly = true)
    public Page<SessionView> sessions(String query, Pageable pageable) {
        return mapSessions(
                userSessionRepository.findActiveSessions(
                        Instant.now().toEpochMilli(), AdminSearch.normalize(query), pageable));
    }

    @Transactional(readOnly = true)
    public Page<SessionView> sessions(
            String query, String clientId, String status, Pageable pageable) {
        long now = Instant.now().toEpochMilli();
        String normalizedQuery = AdminSearch.normalize(query);
        String normalizedStatus = normalizeStatus(status);
        if (clientId == null || clientId.isBlank()) {
            return mapSessions(
                    userSessionRepository.findSessions(
                            now, normalizedQuery, normalizedStatus, pageable));
        }
        var client =
                clientRepository
                        .findByClientId(clientId.trim())
                        .orElseThrow(() -> AdminClientException.notFound("Client not found"));
        List<String> sessionIds =
                authorizationRepository.findDistinctSessionIdsByRegisteredClientId(client.getId());
        if (sessionIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return mapSessions(
                userSessionRepository.findSessionsBySessionIdIn(
                        now, normalizedQuery, normalizedStatus, sessionIds, pageable));
    }

    @Transactional(readOnly = true)
    public SessionDetailView session(String sessionId, String currentUsername) {
        UserSessionEntity session =
                userSessionRepository
                        .findBySessionId(sessionId)
                        .orElseThrow(() -> AdminClientException.notFound("Session not found"));
        adminUserService.assertCanManageUsername(session.getPrincipalName(), currentUsername);
        List<AuthorizationView> authorizations =
                authorizationRepository
                        .findAllBySessionIdOrderByAccessTokenIssuedAtDesc(sessionId)
                        .stream()
                        .map(
                                authorization -> {
                                    var client =
                                            clientRepository
                                                    .findById(authorization.getRegisteredClientId())
                                                    .orElse(null);
                                    return new AuthorizationView(
                                            authorization.getId(),
                                            client == null
                                                    ? authorization.getRegisteredClientId()
                                                    : client.getClientId(),
                                            client == null
                                                    ? authorization.getRegisteredClientId()
                                                    : client.getClientName(),
                                            authorization.getAuthorizationGrantType(),
                                            splitScopes(authorization.getAuthorizedScopes()),
                                            authorization.getAccessTokenIssuedAt(),
                                            authorization.getAccessTokenExpiresAt(),
                                            authorization.getRefreshTokenExpiresAt());
                                })
                        .toList();
        Map<String, Long> counts = Map.of(sessionId, (long) authorizations.size());
        return new SessionDetailView(sessionView(session, counts), authorizations);
    }

    @Transactional(readOnly = true)
    public Page<SessionView> userSessions(Long userId, String currentUsername, Pageable pageable) {
        String username =
                adminUserService.requireManageableUser(userId, currentUsername).getUsername();
        return mapSessions(
                userSessionRepository.findActiveSessionsByPrincipalName(
                        Instant.now().toEpochMilli(), username, pageable));
    }

    @Transactional(readOnly = true)
    public Page<SessionView> clientSessions(String clientId, Pageable pageable) {
        if (!clientRepository.existsById(clientId)) {
            throw AdminClientException.notFound("Client not found");
        }
        List<String> sessionIds =
                authorizationRepository.findDistinctSessionIdsByRegisteredClientId(clientId);
        if (sessionIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return mapSessions(
                userSessionRepository.findActiveSessionsBySessionIdIn(
                        Instant.now().toEpochMilli(), sessionIds, pageable));
    }

    private Page<SessionView> mapSessions(Page<UserSessionEntity> sessions) {
        List<String> sessionIds =
                sessions.getContent().stream().map(UserSessionEntity::getSessionId).toList();
        Map<String, Long> authorizationCounts =
                sessionIds.isEmpty()
                        ? Map.of()
                        : authorizationRepository.countBySessionIdIn(sessionIds).stream()
                                .collect(
                                        java.util.stream.Collectors.toMap(
                                                AuthorizationRepository.SessionAuthorizationCount
                                                        ::getSessionId,
                                                AuthorizationRepository.SessionAuthorizationCount
                                                        ::getAuthorizationCount));
        return sessions.map(session -> sessionView(session, authorizationCounts));
    }

    @Transactional
    public void deleteSession(String sessionId, String currentUsername) {
        UserSessionEntity session =
                userSessionRepository
                        .findBySessionId(sessionId)
                        .orElseThrow(() -> AdminClientException.notFound("Session not found"));
        adminUserService.assertCanManageUsername(session.getPrincipalName(), currentUsername);
        userSessionRepository.deleteBySessionId(sessionId);
        authorizationRepository.deleteBySessionId(sessionId);
        adminAuditEventService.record("session.deleted", "session", sessionId);
    }

    @Transactional
    public void deleteUserSessions(String username, String currentUsername) {
        adminUserService.assertCanManageUsername(username, currentUsername);
        userSessionRepository.deleteByPrincipalName(username);
        authorizationRepository.deleteByPrincipalName(username);
        adminAuditEventService.record("user.sessions.deleted", "user", username);
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "active";
        }
        String normalized = status.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "active", "expired", "all" -> normalized;
            default -> "active";
        };
    }

    private static List<String> splitScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(scopes.split("[, ]+"))
                .filter(scope -> !scope.isBlank())
                .distinct()
                .toList();
    }

    private static SessionView sessionView(
            UserSessionEntity session, Map<String, Long> authorizationCounts) {
        return new SessionView(
                session.getSessionId(),
                session.getPrincipalName(),
                Instant.ofEpochMilli(session.getCreationTime()),
                Instant.ofEpochMilli(session.getLastAccessTime()),
                Instant.ofEpochMilli(session.getExpiryTime()),
                authorizationCounts.getOrDefault(session.getSessionId(), 0L),
                session.getExpiryTime() > Instant.now().toEpochMilli());
    }

    public record SessionView(
            String id,
            String username,
            Instant createdAt,
            Instant lastAccessedAt,
            Instant expiresAt,
            long authorizationCount,
            boolean active) {
        public SessionView(
                String id,
                String username,
                Instant createdAt,
                Instant lastAccessedAt,
                Instant expiresAt,
                long authorizationCount) {
            this(id, username, createdAt, lastAccessedAt, expiresAt, authorizationCount, true);
        }
    }

    public record AuthorizationView(
            String id,
            String clientId,
            String clientName,
            String grantType,
            List<String> scopes,
            Instant accessTokenIssuedAt,
            Instant accessTokenExpiresAt,
            Instant refreshTokenExpiresAt) {}

    public record SessionDetailView(SessionView session, List<AuthorizationView> authorizations) {}
}
