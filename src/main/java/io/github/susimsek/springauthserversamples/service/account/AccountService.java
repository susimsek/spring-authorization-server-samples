package io.github.susimsek.springauthserversamples.service.account;

import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentId;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationConsentRepository;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.AdminClientException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuthorizationConsentRepository authorizationConsentRepository;
    private final AuthorizationRepository authorizationRepository;
    private final ClientRepository clientRepository;
    private final AuthorizationServerMapperSupport mapperSupport;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditEventService auditEventService;

    @Transactional(readOnly = true)
    public ProfileView profile(String username) {
        UserEntity user = requireUser(username);
        return new ProfileView(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public ProfileView updateProfile(
            String username, String firstName, String lastName, String email) {
        UserEntity user = requireUser(username);
        user.setFirstName(normalize(firstName));
        user.setLastName(normalize(lastName));
        user.setEmail(normalize(email));
        auditEventService.record("account.profile.updated", "user", user.getId().toString());
        return new ProfileView(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void changePassword(String username, String currentPassword, String newPassword) {
        UserEntity user = requireUser(username);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw AdminClientException.badRequest(
                    "currentPassword",
                    "account_invalid_current_password",
                    "Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw AdminClientException.badRequest(
                    "newPassword",
                    "account_invalid_password",
                    "Password must be at least 8 characters");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw AdminClientException.badRequest(
                    "newPassword", "account_password_unchanged", "New password must be different");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        auditEventService.record("account.password.updated", "user", user.getId().toString());
    }

    @Transactional(readOnly = true)
    public List<SessionView> sessions(String username, String currentSessionId) {
        return userSessionRepository
                .findAllByPrincipalNameAndExpiryTimeAfter(username, Instant.now().toEpochMilli())
                .stream()
                .sorted(Comparator.comparingLong(UserSessionEntity::getLastAccessTime).reversed())
                .map(
                        session -> {
                            List<
                                            io.github.susimsek.springauthserversamples.domain
                                                    .AuthorizationEntity>
                                    authorizations =
                                            authorizationRepository
                                                    .findAllBySessionIdOrderByAccessTokenIssuedAtDesc(
                                                            session.getSessionId());
                            List<String> registeredClientIds =
                                    authorizations.stream()
                                            .map(
                                                    io.github.susimsek.springauthserversamples
                                                                    .domain.AuthorizationEntity
                                                            ::getRegisteredClientId)
                                            .distinct()
                                            .toList();
                            Map<String, String> clientNames =
                                    clientRepository.findAllById(registeredClientIds).stream()
                                            .collect(
                                                    java.util.stream.Collectors.toMap(
                                                            client -> client.getId(),
                                                            client -> client.getClientName()));
                            List<SessionClientView> clients =
                                    registeredClientIds.stream()
                                            .map(
                                                    id ->
                                                            new SessionClientView(
                                                                    id,
                                                                    clientNames.getOrDefault(
                                                                            id, id)))
                                            .toList();
                            return new SessionView(
                                    session.getSessionId(),
                                    Instant.ofEpochMilli(session.getCreationTime()),
                                    Instant.ofEpochMilli(session.getLastAccessTime()),
                                    Instant.ofEpochMilli(session.getExpiryTime()),
                                    session.getSessionId().equals(currentSessionId),
                                    clients);
                        })
                .toList();
    }

    @Transactional
    public boolean deleteSession(String username, String sessionId) {
        UserSessionEntity session =
                userSessionRepository
                        .findBySessionId(sessionId)
                        .orElseThrow(() -> AdminClientException.notFound("Session not found"));
        if (!username.equals(session.getPrincipalName())) {
            throw AdminClientException.forbidden(
                    "account_session_forbidden", "You cannot manage another user's session");
        }
        userSessionRepository.deleteBySessionId(sessionId);
        authorizationRepository.deleteBySessionId(sessionId);
        auditEventService.record("account.session.deleted", "session", sessionId);
        return true;
    }

    @Transactional
    public void deleteOtherSessions(String username, String currentSessionId) {
        sessions(username, currentSessionId).stream()
                .filter(session -> !session.current())
                .forEach(session -> deleteSession(username, session.id()));
    }

    @Transactional
    public void deleteAllSessions(String username) {
        userSessionRepository
                .findAllByPrincipalNameAndExpiryTimeAfter(username, Instant.now().toEpochMilli())
                .forEach(session -> deleteSession(username, session.getSessionId()));
    }

    @Transactional(readOnly = true)
    public List<ApplicationView> applications(String username) {
        List<AuthorizationConsentEntity> consents =
                authorizationConsentRepository.findAllByIdPrincipalName(username);
        Map<String, String> clientNames =
                clientRepository
                        .findAllById(
                                consents.stream()
                                        .map(consent -> consent.getId().getRegisteredClientId())
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        client -> client.getId(),
                                        client -> client.getClientName()));
        return consents.stream()
                .map(
                        consent -> {
                            String clientId = consent.getId().getRegisteredClientId();
                            Set<String> scopes =
                                    mapperSupport.readAuthorities(consent.getAuthorities()).stream()
                                            .map(authority -> authority.getAuthority())
                                            .collect(
                                                    java.util.stream.Collectors
                                                            .toUnmodifiableSet());
                            return new ApplicationView(
                                    clientId,
                                    clientNames.getOrDefault(clientId, clientId),
                                    scopes,
                                    consent.getCreatedAt(),
                                    consent.getUpdatedAt());
                        })
                .sorted(Comparator.comparing(ApplicationView::clientName))
                .toList();
    }

    @Transactional
    public void revokeApplication(String username, String clientId) {
        AuthorizationConsentId id = new AuthorizationConsentId(clientId, username);
        if (!authorizationConsentRepository.existsById(id)) {
            throw AdminClientException.notFound("Application consent not found");
        }
        authorizationConsentRepository.deleteById(id);
        authorizationRepository.deleteByPrincipalNameAndRegisteredClientId(username, clientId);
        auditEventService.record(
                "account.application.revoked", "consent", clientId + ":" + username);
    }

    private UserEntity requireUser(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> AdminClientException.notFound("User not found"));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record ProfileView(
            String username,
            String firstName,
            String lastName,
            String email,
            Instant createdAt,
            Instant updatedAt) {}

    public record SessionView(
            String id,
            Instant createdAt,
            Instant lastAccessedAt,
            Instant expiresAt,
            boolean current,
            List<SessionClientView> clients) {}

    public record SessionClientView(String clientId, String clientName) {}

    public record ApplicationView(
            String clientId,
            String clientName,
            Set<String> scopes,
            Instant createdAt,
            Instant updatedAt) {}
}
