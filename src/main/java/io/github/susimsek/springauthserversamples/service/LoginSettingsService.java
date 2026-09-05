package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.account.LoginSettingsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminLoginSettingsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminLoginSettingsRequestDTO;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.session.JpaIndexedSessionRepository;
import java.time.Duration;
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
                settings.getBruteForceMaxFailures());
    }

    @Transactional
    public AdminLoginSettingsDTO update(AdminLoginSettingsRequestDTO request) {
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

    private LoginSettingsEntity settings() {
        return repository
                .findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Login settings are not initialized"));
    }
}
