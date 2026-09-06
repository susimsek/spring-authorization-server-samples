package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.account.LoginSettingsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminLoginSettingsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminLoginSettingsRequestDTO;
import io.github.susimsek.springauthserversamples.mapper.LoginSettingsMapper;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.session.JpaIndexedSessionRepository;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class LoginSettingsService {

    private static final long SETTINGS_ID = 1L;
    private final LoginSettingsRepository repository;
    private final AdminAuditEventService auditEventService;
    private final JpaIndexedSessionRepository sessionRepository;
    private final LoginSettingsMapper loginSettingsMapper;

    public LoginSettingsService(
            LoginSettingsRepository repository, AdminAuditEventService auditEventService) {
        this(repository, auditEventService, null, Mappers.getMapper(LoginSettingsMapper.class));
    }

    public LoginSettingsService(
            LoginSettingsRepository repository,
            AdminAuditEventService auditEventService,
            JpaIndexedSessionRepository sessionRepository) {
        this(
                repository,
                auditEventService,
                sessionRepository,
                Mappers.getMapper(LoginSettingsMapper.class));
    }

    @Transactional(readOnly = true)
    public LoginSettingsDTO publicLoginSettings() {
        LoginSettingsEntity settings = settings();
        return loginSettingsMapper.toPublicDTO(settings);
    }

    @Transactional(readOnly = true)
    public AdminLoginSettingsDTO adminLoginSettings() {
        LoginSettingsEntity settings = settings();
        return loginSettingsMapper.toAdminDTO(settings);
    }

    @Transactional
    public AdminLoginSettingsDTO update(AdminLoginSettingsRequestDTO request) {
        validateOtpPolicy(request);
        LoginSettingsEntity settings = settings();
        loginSettingsMapper.update(request, settings);
        repository.save(settings);
        if (sessionRepository != null) {
            sessionRepository.setDefaultMaxInactiveInterval(
                    Duration.ofMinutes(settings.getSessionTimeoutMinutes()));
        }
        auditEventService.record("login.settings.updated", "login-settings", "default");
        return loginSettingsMapper.toAdminDTO(settings);
    }

    @Transactional(readOnly = true)
    public boolean isRememberMeEnabled() {
        return settings().isRememberMeEnabled();
    }

    @Transactional(readOnly = true)
    public boolean isUserRegistrationEnabled() {
        return settings().isUserRegistrationEnabled();
    }

    @Transactional(readOnly = true)
    public boolean isForgotPasswordEnabled() {
        return settings().isForgotPasswordEnabled();
    }

    @Transactional(readOnly = true)
    public boolean isLoginWithEmailEnabled() {
        return settings().isLoginWithEmail();
    }

    @Transactional(readOnly = true)
    public boolean isVerifyEmailEnabled() {
        return settings().isVerifyEmail();
    }

    @Transactional(readOnly = true)
    public boolean isBruteForceEnabled() {
        return settings().isBruteForceEnabled();
    }

    @Transactional(readOnly = true)
    public int passwordMinimumLength() {
        return settings().getPasswordMinimumLength();
    }

    @Transactional(readOnly = true)
    public int bruteForceMaxFailures() {
        return settings().getBruteForceMaxFailures();
    }

    @Transactional(readOnly = true)
    public int bruteForceMaxSecondaryFailures() {
        return settings().getBruteForceMaxSecondaryFailures();
    }

    @Transactional(readOnly = true)
    public Duration mfaVerificationTimeout() {
        return Duration.ofSeconds(settings().getMfaVerificationTimeoutSeconds());
    }

    @Transactional(readOnly = true)
    public ApplicationProperties.PasswordPolicy passwordPolicy() {
        LoginSettingsEntity value = settings();
        return new ApplicationProperties.PasswordPolicy(
                value.getPasswordMinimumLength(),
                value.getPasswordMaximumLength(),
                value.getPasswordMinimumUppercase(),
                value.getPasswordMinimumLowercase(),
                value.getPasswordMinimumDigits(),
                value.getPasswordMinimumSpecialCharacters(),
                value.isPasswordRejectUsername(),
                value.isPasswordRejectEmail(),
                value.isPasswordRejectCommonPasswords(),
                value.getPasswordHistorySize(),
                value.getPasswordExpirationDays(),
                value.getPasswordCommonPasswords());
    }

    @Transactional(readOnly = true)
    public ApplicationProperties.BruteForce bruteForcePolicy() {
        LoginSettingsEntity value = settings();
        return new ApplicationProperties.BruteForce(
                value.isBruteForceEnabled(),
                value.getBruteForceMaxFailures(),
                Duration.ofMillis(value.getBruteForceQuickLoginWindowMillis()),
                Duration.ofSeconds(value.getBruteForceMinimumQuickLoginWaitSeconds()),
                Duration.ofSeconds(value.getBruteForceWaitIncrementSeconds()),
                Duration.ofSeconds(value.getBruteForceMaxWaitSeconds()),
                Duration.ofSeconds(value.getBruteForceFailureResetTimeSeconds()),
                value.getBruteForceMaxTemporaryLockouts(),
                value.isBruteForcePermanentLockout(),
                value.getBruteForceIpRequestsPerMinute(),
                value.getBruteForceUsernameIpRequestsPerMinute());
    }

    @Transactional(readOnly = true)
    public boolean isOtpAddRecoveryCodesEnabled() {
        return settings().isOtpAddRecoveryCodes();
    }

    @Transactional(readOnly = true)
    public boolean isOtpEnabled() {
        return settings().isOtpEnabled();
    }

    @Transactional(readOnly = true)
    public boolean isOtpRequired() {
        return settings().isOtpRequired();
    }

    @Transactional(readOnly = true)
    public String otpIssuer() {
        return settings().getOtpIssuer();
    }

    @Transactional(readOnly = true)
    public String otpAlgorithm() {
        return settings().getOtpAlgorithm();
    }

    @Transactional(readOnly = true)
    public int otpDigits() {
        return settings().getOtpDigits();
    }

    @Transactional(readOnly = true)
    public int otpPeriodSeconds() {
        return settings().getOtpPeriodSeconds();
    }

    @Transactional(readOnly = true)
    public int otpLookAheadWindow() {
        return settings().getOtpLookAheadWindow();
    }

    @Transactional(readOnly = true)
    public boolean isOtpCodeReusable() {
        return settings().isOtpCodeReusable();
    }

    private LoginSettingsEntity settings() {
        return repository
                .findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Login settings are not initialized"));
    }

    private static void validateOtpPolicy(AdminLoginSettingsRequestDTO request) {
        if (request.otpRequired() && !request.otpEnabled()) {
            throw io.github.susimsek.springauthserversamples.service.error.ApiException.badRequest(
                    io.github.susimsek.springauthserversamples.service.error.ApiErrorCode
                            .INVALID_REQUEST,
                    "OTP cannot be required when it is disabled");
        }
        if (request.otpIssuer() == null || request.otpIssuer().isBlank()) {
            throw io.github.susimsek.springauthserversamples.service.error.ApiException.badRequest(
                    io.github.susimsek.springauthserversamples.service.error.ApiErrorCode
                            .INVALID_REQUEST,
                    "OTP issuer is required");
        }
        String algorithm = request.otpAlgorithm();
        if (algorithm == null
                || !java.util.Set.of("SHA1", "SHA256", "SHA512")
                        .contains(algorithm.toUpperCase(Locale.ROOT))) {
            throw io.github.susimsek.springauthserversamples.service.error.ApiException.badRequest(
                    io.github.susimsek.springauthserversamples.service.error.ApiErrorCode
                            .INVALID_REQUEST,
                    "OTP algorithm is invalid");
        }
        if (request.otpDigits() != 6 && request.otpDigits() != 8) {
            throw io.github.susimsek.springauthserversamples.service.error.ApiException.badRequest(
                    io.github.susimsek.springauthserversamples.service.error.ApiErrorCode
                            .INVALID_REQUEST,
                    "OTP digits must be 6 or 8");
        }
    }
}
