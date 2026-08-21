package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
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
    private final AdminAuditEventService adminAuditEventService;

    @Transactional(readOnly = true)
    public Page<SessionView> sessions(String query, Pageable pageable) {
        Page<UserSessionEntity> sessions =
                userSessionRepository.findActiveSessions(
                        Instant.now().toEpochMilli(), AdminSearch.normalize(query), pageable);
        List<String> principalNames =
                sessions.getContent().stream()
                        .map(UserSessionEntity::getPrincipalName)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();
        Map<String, Long> authorizationCounts =
                principalNames.isEmpty()
                        ? Map.of()
                        : authorizationRepository.countByPrincipalNameIn(principalNames).stream()
                                .collect(
                                        java.util.stream.Collectors.toMap(
                                                AuthorizationRepository.AuthorizationCount
                                                        ::getPrincipalName,
                                                AuthorizationRepository.AuthorizationCount
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

    private static SessionView sessionView(
            UserSessionEntity session, Map<String, Long> authorizationCounts) {
        return new SessionView(
                session.getSessionId(),
                session.getPrincipalName(),
                Instant.ofEpochMilli(session.getCreationTime()),
                Instant.ofEpochMilli(session.getLastAccessTime()),
                Instant.ofEpochMilli(session.getExpiryTime()),
                authorizationCounts.getOrDefault(session.getPrincipalName(), 0L));
    }

    public record SessionView(
            String id,
            String username,
            Instant createdAt,
            Instant lastAccessedAt,
            Instant expiresAt,
            long authorizationCount) {}
}
