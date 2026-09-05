package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.account.LoginSettingsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminLoginSettingsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminLoginSettingsRequestDTO;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.session.JpaIndexedSessionRepository;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class LoginSettingsService {

    private static final long SETTINGS_ID = 1L;
    private final LoginSettingsRepository repository;
    private final AdminAuditEventService auditEventService;
    private final JpaIndexedSessionRepository sessionRepository;

    public LoginSettingsService(
            LoginSettingsRepository repository, AdminAuditEventService auditEventService) {
        this(repository, auditEventService, null);
    }

    @Transactional(readOnly = true)
    public LoginSettingsDTO publicLoginSettings() {
        LoginSettingsEntity settings = settings();
        return new LoginSettingsDTO(
                settings.isUserRegistrationEnabled(),
                settings.isForgotPasswordEnabled(),
                settings.isRememberMeEnabled());
    }

    @Transactional(readOnly = true)
    public AdminLoginSettingsDTO adminLoginSettings() {
        LoginSettingsEntity settings = settings();
        return new AdminLoginSettingsDTO(
                settings.isUserRegistrationEnabled(),
                settings.isForgotPasswordEnabled(),
                settings.isRememberMeEnabled(),
                settings.isLoginWithEmail(),
                settings.isVerifyEmail(),
                settings.getSessionTimeoutMinutes(),
                settings.getPasswordMinimumLength(),
                settings.isBruteForceEnabled(),
                settings.getBruteForceMaxFailures(),
                settings.isOtpEnabled(),
                settings.isOtpRequired(),
                settings.getOtpIssuer(),
                settings.getOtpAlgorithm(),
                settings.getOtpDigits(),
                settings.getOtpPeriodSeconds(),
                settings.getOtpLookAheadWindow());
    }

    @Transactional
    public AdminLoginSettingsDTO update(AdminLoginSettingsRequestDTO request) {
        validateOtpPolicy(request);
        LoginSettingsEntity settings = settings();
        settings.setUserRegistrationEnabled(request.userRegistration());
        settings.setForgotPasswordEnabled(request.forgotPassword());
        settings.setRememberMeEnabled(request.rememberMe());
        settings.setLoginWithEmail(request.loginWithEmail());
        settings.setVerifyEmail(request.verifyEmail());
        settings.setSessionTimeoutMinutes(request.sessionTimeoutMinutes());
        settings.setPasswordMinimumLength(request.passwordMinimumLength());
        settings.setBruteForceEnabled(request.bruteForceEnabled());
        settings.setBruteForceMaxFailures(request.bruteForceMaxFailures());
        settings.setOtpEnabled(request.otpEnabled());
        settings.setOtpRequired(request.otpRequired());
        settings.setOtpIssuer(request.otpIssuer().trim());
        settings.setOtpAlgorithm(request.otpAlgorithm().toUpperCase(Locale.ROOT));
        settings.setOtpDigits(request.otpDigits());
        settings.setOtpPeriodSeconds(request.otpPeriodSeconds());
        settings.setOtpLookAheadWindow(request.otpLookAheadWindow());
        repository.save(settings);
        if (sessionRepository != null) {
            sessionRepository.setDefaultMaxInactiveInterval(
                    Duration.ofMinutes(settings.getSessionTimeoutMinutes()));
        }
        auditEventService.record("login.settings.updated", "login-settings", "default");
        return adminLoginSettings();
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
