package io.github.susimsek.springauthserversamples.service.account;

import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentId;
import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import io.github.susimsek.springauthserversamples.dto.account.AccountApplicationDTO;
import io.github.susimsek.springauthserversamples.dto.account.AccountProfileDTO;
import io.github.susimsek.springauthserversamples.dto.account.AccountProfileRequestDTO;
import io.github.susimsek.springauthserversamples.dto.account.AccountSessionClientDTO;
import io.github.susimsek.springauthserversamples.dto.account.AccountSessionDTO;
import io.github.susimsek.springauthserversamples.mapper.AccountProfileMapper;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationConsentRepository;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final AccountProfileMapper accountProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditEventService auditEventService;

    @Transactional(readOnly = true)
    public AccountProfileDTO profile(String username) {
        return accountProfileMapper.toDTO(requireUser(username));
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public AccountProfileDTO updateProfile(String username, AccountProfileRequestDTO request) {
        UserEntity user = requireUser(username);
        accountProfileMapper.updateEntity(normalized(request), user);
        auditEventService.record("account.profile.updated", "user", user.getId().toString());
        return accountProfileMapper.toDTO(user);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void changePassword(String username, String currentPassword, String newPassword) {
        UserEntity user = requireUser(username);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw ApiException.badRequest(
                    "currentPassword",
                    "account_invalid_current_password",
                    "Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw ApiException.badRequest(
                    "newPassword",
                    "account_invalid_password",
                    "Password must be at least 8 characters");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw ApiException.badRequest(
                    "newPassword", "account_password_unchanged", "New password must be different");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        auditEventService.record("account.password.updated", "user", user.getId().toString());
    }

    @Transactional(readOnly = true)
    public Page<AccountSessionDTO> sessions(
            String username, String currentSessionId, Pageable pageable) {
        Page<UserSessionEntity> sessions =
                userSessionRepository.findActiveSessionsByPrincipalName(
                        Instant.now().toEpochMilli(), username, pageable);
        return sessionViews(sessions, currentSessionId);
    }

    @Transactional
    public boolean deleteSession(String username, String sessionId) {
        UserSessionEntity session =
                userSessionRepository
                        .findBySessionId(sessionId)
                        .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (!username.equals(session.getPrincipalName())) {
            throw ApiException.forbidden(
                    "account_session_forbidden", "You cannot manage another user's session");
        }
        userSessionRepository.deleteBySessionId(sessionId);
        authorizationRepository.deleteBySessionId(sessionId);
        auditEventService.record("account.session.deleted", "session", sessionId);
        return true;
    }

    @Transactional
    public void deleteOtherSessions(String username, String currentSessionId) {
        List<String> sessionIds =
                userSessionRepository
                        .findAllByPrincipalNameAndExpiryTimeAfter(
                                username, Instant.now().toEpochMilli())
                        .stream()
                        .map(UserSessionEntity::getSessionId)
                        .filter(sessionId -> !sessionId.equals(currentSessionId))
                        .toList();
        if (sessionIds.isEmpty()) {
            return;
        }
        userSessionRepository.deleteBySessionIdIn(sessionIds);
        authorizationRepository.deleteBySessionIdIn(sessionIds);
        sessionIds.forEach(
                sessionId ->
                        auditEventService.record("account.session.deleted", "session", sessionId));
    }

    @Transactional
    public void deleteAllSessions(String username) {
        userSessionRepository
                .findAllByPrincipalNameAndExpiryTimeAfter(username, Instant.now().toEpochMilli())
                .forEach(session -> deleteSession(username, session.getSessionId()));
    }

    @Transactional(readOnly = true)
    public Page<AccountApplicationDTO> applications(String username, Pageable pageable) {
        Page<AuthorizationConsentEntity> consents =
                authorizationConsentRepository.findByIdPrincipalName(username, pageable);
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
        return consents.map(
                consent -> {
                    String clientId = consent.getId().getRegisteredClientId();
                    Set<String> scopes =
                            mapperSupport.readAuthorities(consent.getAuthorities()).stream()
                                    .map(authority -> authority.getAuthority())
                                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
                    return new AccountApplicationDTO(
                            clientId,
                            clientNames.getOrDefault(clientId, clientId),
                            scopes,
                            consent.getCreatedAt(),
                            consent.getUpdatedAt());
                });
    }

    @Transactional
    public void revokeApplication(String username, String clientId) {
        AuthorizationConsentId id = new AuthorizationConsentId(clientId, username);
        if (!authorizationConsentRepository.existsById(id)) {
            throw ApiException.notFound("Application consent not found");
        }
        authorizationConsentRepository.deleteById(id);
        authorizationRepository.deleteByPrincipalNameAndRegisteredClientId(username, clientId);
        auditEventService.record(
                "account.application.revoked", "consent", clientId + ":" + username);
    }

    private UserEntity requireUser(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private Page<AccountSessionDTO> sessionViews(
            Page<UserSessionEntity> sessions, String currentSessionId) {
        List<String> sessionIds =
                sessions.getContent().stream().map(UserSessionEntity::getSessionId).toList();
        if (sessionIds.isEmpty()) {
            return sessions.map(
                    session -> sessionView(session, currentSessionId, List.of(), Map.of()));
        }
        Map<String, List<AuthorizationEntity>> authorizationsBySessionId =
                authorizationRepository
                        .findAllBySessionIdInOrderByAccessTokenIssuedAtDesc(sessionIds)
                        .stream()
                        .collect(
                                java.util.stream.Collectors.groupingBy(
                                        AuthorizationEntity::getSessionId));
        Map<String, String> clientNames =
                clientRepository
                        .findAllById(
                                authorizationsBySessionId.values().stream()
                                        .flatMap(List::stream)
                                        .map(AuthorizationEntity::getRegisteredClientId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        client -> client.getId(),
                                        client -> client.getClientName()));
        return sessions.map(
                session ->
                        sessionView(
                                session,
                                currentSessionId,
                                authorizationsBySessionId.getOrDefault(
                                        session.getSessionId(), List.of()),
                                clientNames));
    }

    private static AccountSessionDTO sessionView(
            UserSessionEntity session,
            String currentSessionId,
            List<AuthorizationEntity> authorizations,
            Map<String, String> clientNames) {
        List<AccountSessionClientDTO> clients =
                authorizations.stream()
                        .map(AuthorizationEntity::getRegisteredClientId)
                        .distinct()
                        .map(
                                clientId ->
                                        new AccountSessionClientDTO(
                                                clientId,
                                                clientNames.getOrDefault(clientId, clientId)))
                        .toList();
        return new AccountSessionDTO(
                session.getSessionId(),
                Instant.ofEpochMilli(session.getCreationTime()),
                Instant.ofEpochMilli(session.getLastAccessTime()),
                Instant.ofEpochMilli(session.getExpiryTime()),
                session.getSessionId().equals(currentSessionId),
                clients);
    }

    private static AccountProfileRequestDTO normalized(AccountProfileRequestDTO request) {
        return new AccountProfileRequestDTO(
                normalize(request.firstName()),
                normalize(request.lastName()),
                normalize(request.email()));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
