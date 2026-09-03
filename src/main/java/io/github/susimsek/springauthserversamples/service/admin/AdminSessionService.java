package io.github.susimsek.springauthserversamples.service.admin;

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
    private final SessionInvalidationService sessionInvalidationService;

    @Transactional(readOnly = true)
    public Page<AdminSessionDTO> sessions(
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
                        .orElseThrow(() -> ApiException.notFound("Client not found"));
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
    public AdminSessionDetailDTO session(String sessionId, String currentUsername) {
        UserSessionEntity session =
                userSessionRepository
                        .findBySessionId(sessionId)
                        .orElseThrow(() -> ApiException.notFound("Session not found"));
        adminUserService.assertCanManageUsername(session.getPrincipalName(), currentUsername);
        List<AuthorizationEntity> authorizations =
                authorizationRepository.findAllBySessionIdOrderByAccessTokenIssuedAtDesc(sessionId);
        Map<String, RegisteredClientEntity> clients =
                clientRepository
                        .findAllById(
                                authorizations.stream()
                                        .map(AuthorizationEntity::getRegisteredClientId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        client -> client.getId(), client -> client));
        List<AdminAuthorizationDTO> authorizationViews =
                authorizations.stream()
                        .map(
                                authorization -> {
                                    var client = clients.get(authorization.getRegisteredClientId());
                                    return new AdminAuthorizationDTO(
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
        Map<String, Long> counts = Map.of(sessionId, (long) authorizationViews.size());
        return new AdminSessionDetailDTO(sessionView(session, counts), authorizationViews);
    }

    @Transactional(readOnly = true)
    public Page<AdminSessionDTO> userSessions(
            Long userId, String currentUsername, Pageable pageable) {
        String username =
                adminUserService.requireManageableUser(userId, currentUsername).getUsername();
        return mapSessions(
                userSessionRepository.findActiveSessionsByPrincipalName(
                        Instant.now().toEpochMilli(), username, pageable));
    }

    @Transactional(readOnly = true)
    public Page<AdminSessionDTO> clientSessions(String clientId, Pageable pageable) {
        if (!clientRepository.existsById(clientId)) {
            throw ApiException.notFound("Client not found");
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

    private Page<AdminSessionDTO> mapSessions(Page<UserSessionEntity> sessions) {
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
                        .orElseThrow(() -> ApiException.notFound("Session not found"));
        adminUserService.assertCanManageUsername(session.getPrincipalName(), currentUsername);
        sessionInvalidationService.invalidateSession(sessionId);
        adminAuditEventService.record("session.deleted", "session", sessionId);
    }

    @Transactional
    public void deleteUserSessions(String username, String currentUsername) {
        adminUserService.assertCanManageUsername(username, currentUsername);
        sessionInvalidationService.invalidatePrincipal(username);
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

    private static AdminSessionDTO sessionView(
            UserSessionEntity session, Map<String, Long> authorizationCounts) {
        return new AdminSessionDTO(
                session.getSessionId(),
                session.getPrincipalName(),
                Instant.ofEpochMilli(session.getCreationTime()),
                Instant.ofEpochMilli(session.getLastAccessTime()),
                Instant.ofEpochMilli(session.getExpiryTime()),
                authorizationCounts.getOrDefault(session.getSessionId(), 0L),
                session.getExpiryTime() > Instant.now().toEpochMilli());
    }
}
