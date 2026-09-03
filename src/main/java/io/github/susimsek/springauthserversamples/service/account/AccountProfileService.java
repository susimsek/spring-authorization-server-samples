package io.github.susimsek.springauthserversamples.service.account;

import io.github.susimsek.springauthserversamples.domain.UserAction;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.account.AccountProfileDTO;
import io.github.susimsek.springauthserversamples.dto.account.AccountProfileRequestDTO;
import io.github.susimsek.springauthserversamples.mapper.AccountProfileMapper;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountProfileService {

    private final UserRepository userRepository;
    private final AccountProfileMapper accountProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditEventService auditEventService;
    private final UserAccessInvalidationService userAccessInvalidationService;
    private final UserActionService userActionService;

    @Transactional(readOnly = true)
    public AccountProfileDTO profile(String username) {
        return accountProfileMapper.toDTO(requireUser(username));
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, key = "#username")
    public AccountProfileDTO updateProfile(String username, AccountProfileRequestDTO request) {
        UserEntity user = requireUser(username);
        AccountProfileRequestDTO normalized = normalized(request);
        String email = normalized.email();
        if (email != null && userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
            throw ApiException.conflict(
                    "email", ApiErrorCode.USER_DUPLICATE_EMAIL, "Email is already registered");
        }
        boolean emailChanged = !java.util.Objects.equals(user.getEmail(), email);
        accountProfileMapper.updateEntity(normalized, user);
        if (emailChanged) {
            user.setEmailVerified(false);
            userActionService.invalidateActions(user.getId());
        }
        userRepository.save(user);
        auditEventService.record("account.profile.updated", "user", user.getId().toString());
        return accountProfileMapper.toDTO(user);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, key = "#username")
    public void changePassword(String username, String currentPassword, String newPassword) {
        UserEntity user = requireUser(username);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw ApiException.badRequest(
                    "currentPassword",
                    ApiErrorCode.INVALID_CURRENT_PASSWORD,
                    "Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw ApiException.badRequest(
                    "newPassword",
                    ApiErrorCode.INVALID_PASSWORD,
                    "Password must be at least 8 characters");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw ApiException.badRequest(
                    "newPassword",
                    ApiErrorCode.PASSWORD_UNCHANGED,
                    "New password must be different");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userActionService.invalidateActions(user.getId());
        userRepository.save(user);
        userAccessInvalidationService.invalidate(username);
        auditEventService.record("account.password.updated", "user", user.getId().toString());
    }

    @Transactional
    public void sendVerificationEmail(String username, java.util.Locale locale) {
        userActionService.sendForCurrentUser(username, UserAction.VERIFY_EMAIL, locale);
    }

    private UserEntity requireUser(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private static AccountProfileRequestDTO normalized(AccountProfileRequestDTO request) {
        return new AccountProfileRequestDTO(
                normalize(request.firstName()),
                normalize(request.lastName()),
                normalizeEmail(request.email()));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeEmail(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase(java.util.Locale.ROOT);
    }
}
