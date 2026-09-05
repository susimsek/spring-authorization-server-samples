package io.github.susimsek.springauthserversamples.service.account;

import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.account.MfaSetupDTO;
import io.github.susimsek.springauthserversamples.dto.account.MfaStatusDTO;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import io.github.susimsek.springauthserversamples.service.security.TotpService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final UserRepository userRepository;
    private final LoginSettingsRepository loginSettingsRepository;
    private final TotpService totpService;
    private final AdminAuditEventService auditEventService;
    private final UserAccessInvalidationService userAccessInvalidationService;

    @Transactional(readOnly = true)
    public MfaStatusDTO status(String username) {
        UserEntity user = user(username);
        LoginSettingsEntity settings = settings();
        return new MfaStatusDTO(
                user.isTotpEnabled(),
                settings.isOtpEnabled(),
                settings.isOtpRequired(),
                settings.getOtpIssuer(),
                settings.getOtpAlgorithm(),
                settings.getOtpDigits(),
                settings.getOtpPeriodSeconds());
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public MfaSetupDTO setup(String username) {
        LoginSettingsEntity settings = settings();
        if (!settings.isOtpEnabled()) {
            throw ApiException.badRequest(
                    ApiErrorCode.INVALID_REQUEST, "TOTP enrollment is disabled");
        }
        UserEntity user = user(username);
        String secret = user.getTotpSecret();
        if (secret == null || user.isTotpEnabled()) {
            secret = totpService.newSecret();
            user.setTotpSecret(secret);
            user.setTotpEnabled(false);
            userRepository.save(user);
        }
        return new MfaSetupDTO(
                secret,
                totpService.otpauthUri(
                        settings.getOtpIssuer(),
                        user.getUsername(),
                        secret,
                        settings.getOtpAlgorithm(),
                        settings.getOtpDigits(),
                        settings.getOtpPeriodSeconds()),
                settings.getOtpAlgorithm(),
                settings.getOtpDigits(),
                settings.getOtpPeriodSeconds());
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void enable(String username, String code) {
        LoginSettingsEntity settings = settings();
        UserEntity user = user(username);
        if (!settings.isOtpEnabled()
                || user.getTotpSecret() == null
                || !totpService.matches(
                        user.getTotpSecret(),
                        code,
                        settings.getOtpAlgorithm(),
                        settings.getOtpDigits(),
                        settings.getOtpPeriodSeconds(),
                        settings.getOtpLookAheadWindow())) {
            throw ApiException.badRequest(
                    ApiErrorCode.INVALID_TOTP_CODE, "The authenticator code is invalid");
        }
        user.setTotpEnabled(true);
        userRepository.save(user);
        userAccessInvalidationService.invalidate(user.getUsername());
        auditEventService.record("account.mfa.enabled", "user", user.getId().toString());
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void disable(String username, String code) {
        LoginSettingsEntity settings = settings();
        UserEntity user = user(username);
        if (user.getTotpSecret() == null
                || !totpService.matches(
                        user.getTotpSecret(),
                        code,
                        settings.getOtpAlgorithm(),
                        settings.getOtpDigits(),
                        settings.getOtpPeriodSeconds(),
                        settings.getOtpLookAheadWindow())) {
            throw ApiException.badRequest(
                    ApiErrorCode.INVALID_TOTP_CODE, "The authenticator code is invalid");
        }
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        userRepository.save(user);
        userAccessInvalidationService.invalidate(user.getUsername());
        auditEventService.record("account.mfa.disabled", "user", user.getId().toString());
    }

    @Transactional(readOnly = true)
    public boolean valid(String username, String code) {
        LoginSettingsEntity settings = settings();
        UserEntity user = user(username);
        return user.isTotpEnabled()
                && totpService.matches(
                        user.getTotpSecret(),
                        code,
                        settings.getOtpAlgorithm(),
                        settings.getOtpDigits(),
                        settings.getOtpPeriodSeconds(),
                        settings.getOtpLookAheadWindow());
    }

    private UserEntity user(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private LoginSettingsEntity settings() {
        return loginSettingsRepository
                .findById(1L)
                .orElseThrow(() -> new IllegalStateException("Login settings are not initialized"));
    }
}
