package io.github.susimsek.springauthserversamples.service.security;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.LoginSettingsService;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class AccountLockService {

    private final UserRepository userRepository;
    private final UserAccessInvalidationService userAccessInvalidationService;
    private final ApplicationProperties applicationProperties;
    private final LoginSettingsService loginSettingsService;

    public AccountLockService(
            UserRepository userRepository,
            UserAccessInvalidationService userAccessInvalidationService,
            ApplicationProperties applicationProperties) {
        this(userRepository, userAccessInvalidationService, applicationProperties, null);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void recordFailure(String username, String ipAddress) {
        if ((!applicationProperties.security().bruteForce().enabled()
                        && loginSettingsService == null)
                || (loginSettingsService != null && !loginSettingsService.isBruteForceEnabled())
                || username == null
                || username.isBlank()) {
            return;
        }
        userRepository
                .findForLoginUpdate(username.trim())
                .ifPresent(user -> recordFailure(user, Instant.now(), ipAddress));
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void recordSuccess(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        userRepository.findForLoginUpdate(username.trim()).ifPresent(this::resetFailures);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void unlock(Long userId) {
        userRepository
                .findForActionById(userId)
                .ifPresent(
                        user -> {
                            resetFailures(user);
                            user.setMfaFailedAttemptCount(0);
                            user.setMfaPermanentlyLocked(false);
                        });
    }

    public boolean isLocked(UserEntity user, Instant now) {
        return user.isPermanentlyLocked()
                || user.isMfaPermanentlyLocked()
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now));
    }

    private void recordFailure(UserEntity user, Instant now, String ipAddress) {
        ApplicationProperties.BruteForce policy = applicationProperties.security().bruteForce();
        if (!user.isEnabled() || user.isPermanentlyLocked()) {
            return;
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            return;
        }
        Instant previousFailure = user.getLastFailedLoginAt();
        if (previousFailure == null
                || previousFailure.plus(policy.failureResetTime()).isBefore(now)) {
            user.setFailedLoginCount(0);
            user.setTemporaryLockoutCount(0);
        }
        int failures = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(failures);
        user.setLastFailedLoginAt(now);
        int maxFailures =
                loginSettingsService == null
                        ? policy.maxFailures()
                        : loginSettingsService.bruteForceMaxFailures();
        if (failures < Math.max(1, maxFailures)) {
            if (previousFailure != null
                    && now.minus(policy.quickLoginWindow()).isBefore(previousFailure)) {
                user.setLockedUntil(now.plus(policy.minimumQuickLoginWait()));
            }
            return;
        }
        int temporaryLockouts = user.getTemporaryLockoutCount() + 1;
        user.setTemporaryLockoutCount(temporaryLockouts);
        boolean permanent =
                policy.permanentLockout()
                        || (policy.maxTemporaryLockouts() > 0
                                && temporaryLockouts > policy.maxTemporaryLockouts());
        if (permanent) {
            user.setPermanentlyLocked(true);
            user.setLockedUntil(null);
            userAccessInvalidationService.invalidate(user.getUsername());
            return;
        }
        Duration wait =
                policy.minimumQuickLoginWait()
                        .plus(
                                policy.waitIncrement()
                                        .multipliedBy(Math.max(0, temporaryLockouts - 1L)));
        user.setLockedUntil(
                now.plus(wait.compareTo(policy.maxWait()) > 0 ? policy.maxWait() : wait));
    }

    private void resetFailures(UserEntity user) {
        user.setFailedLoginCount(0);
        user.setLastFailedLoginAt(null);
        user.setLockedUntil(null);
        user.setTemporaryLockoutCount(0);
        user.setPermanentlyLocked(false);
    }
}
