package io.github.susimsek.springauthserversamples.service.security;

import io.github.susimsek.springauthserversamples.config.security.SocialLoginSecretCipher;
import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRegistrationCaptchaDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRegistrationCaptchaRequestDTO;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationCaptchaSettingsService {

    private static final long SETTINGS_ID = 1L;
    private static final Pattern ACTION_PATTERN = Pattern.compile("[A-Za-z0-9/_]+");
    private static final Set<String> PROVIDERS = Set.of("recaptcha", "enterprise");

    private final LoginSettingsRepository repository;
    private final SocialLoginSecretCipher secretCipher;
    private final AdminAuditEventService auditEventService;

    @Transactional(readOnly = true)
    public AdminRegistrationCaptchaDTO adminSettings() {
        LoginSettingsEntity settings = settings();
        return new AdminRegistrationCaptchaDTO(
                settings.isRegistrationCaptchaEnabled(),
                provider(settings),
                value(settings.getRegistrationCaptchaSiteKey()),
                value(settings.getRegistrationCaptchaProjectId()),
                action(settings.getRegistrationCaptchaAction(), "register"),
                settings.isRegistrationCaptchaV3(),
                settings.getRegistrationCaptchaScoreThreshold(),
                settings.isRegistrationCaptchaUseRecaptchaNet(),
                present(settings.getRegistrationCaptchaSecretEncrypted()),
                present(settings.getRegistrationCaptchaApiKeyEncrypted()),
                settings.isLoginCaptchaEnabled(),
                action(settings.getLoginCaptchaAction(), "login"),
                settings.isLoginCaptchaV3(),
                settings.getLoginCaptchaScoreThreshold());
    }

    @Transactional(readOnly = true)
    public RegistrationCaptchaConfiguration publicConfiguration() {
        LoginSettingsEntity settings = settings();
        return configuration(
                settings,
                present(settings.getRegistrationCaptchaSecretEncrypted()) ? "configured" : "",
                present(settings.getRegistrationCaptchaApiKeyEncrypted()) ? "configured" : "");
    }

    @Transactional(readOnly = true)
    public RegistrationCaptchaConfiguration loginPublicConfiguration() {
        LoginSettingsEntity settings = settings();
        return configuration(
                settings,
                present(settings.getRegistrationCaptchaSecretEncrypted()) ? "configured" : "",
                present(settings.getRegistrationCaptchaApiKeyEncrypted()) ? "configured" : "",
                settings.isLoginCaptchaEnabled(),
                action(settings.getLoginCaptchaAction(), "login"),
                settings.isLoginCaptchaV3(),
                settings.getLoginCaptchaScoreThreshold());
    }

    @Transactional(readOnly = true)
    public RegistrationCaptchaConfiguration verificationConfiguration() {
        LoginSettingsEntity settings = settings();
        return configuration(
                settings,
                decrypt(settings.getRegistrationCaptchaSecretEncrypted()),
                decrypt(settings.getRegistrationCaptchaApiKeyEncrypted()));
    }

    @Transactional(readOnly = true)
    public RegistrationCaptchaConfiguration loginVerificationConfiguration() {
        LoginSettingsEntity settings = settings();
        return configuration(
                settings,
                decrypt(settings.getRegistrationCaptchaSecretEncrypted()),
                decrypt(settings.getRegistrationCaptchaApiKeyEncrypted()),
                settings.isLoginCaptchaEnabled(),
                action(settings.getLoginCaptchaAction(), "login"),
                settings.isLoginCaptchaV3(),
                settings.getLoginCaptchaScoreThreshold());
    }

    @Transactional
    public AdminRegistrationCaptchaDTO update(AdminRegistrationCaptchaRequestDTO request) {
        String provider = normalizeProvider(request.provider());
        if (!PROVIDERS.contains(provider)) {
            throw invalid("provider", "CAPTCHA provider is invalid");
        }
        String action = normalizeAction(request.action(), "register");
        String loginAction = normalizeAction(request.loginAction(), "login");

        LoginSettingsEntity settings = settings();
        validateConfiguration(request, settings, provider, action, loginAction);
        settings.setRegistrationCaptchaEnabled(request.enabled());
        settings.setRegistrationCaptchaProvider(provider);
        settings.setRegistrationCaptchaSiteKey(trimToNull(request.siteKey()));
        settings.setRegistrationCaptchaProjectId(trimToNull(request.projectId()));
        settings.setRegistrationCaptchaAction(action);
        settings.setRegistrationCaptchaV3(request.recaptchaV3());
        settings.setRegistrationCaptchaScoreThreshold(request.scoreThreshold());
        settings.setRegistrationCaptchaUseRecaptchaNet(request.useRecaptchaNet());
        settings.setLoginCaptchaEnabled(request.loginEnabled());
        settings.setLoginCaptchaAction(loginAction);
        settings.setLoginCaptchaV3(request.loginRecaptchaV3());
        settings.setLoginCaptchaScoreThreshold(request.loginScoreThreshold());
        if (present(request.secretKey())) {
            settings.setRegistrationCaptchaSecretEncrypted(
                    encrypt(request.secretKey(), "secretKey"));
        }
        if (present(request.apiKey())) {
            settings.setRegistrationCaptchaApiKeyEncrypted(encrypt(request.apiKey(), "apiKey"));
        }
        repository.save(settings);
        auditEventService.record("registration.captcha.updated", "registration-captcha", "default");
        return adminSettings();
    }

    private void validateConfiguration(
            AdminRegistrationCaptchaRequestDTO request,
            LoginSettingsEntity settings,
            String provider,
            String action,
            String loginAction) {
        validateEnabledConfiguration(
                request.enabled(),
                request.siteKey(),
                request.projectId(),
                request.secretKey(),
                request.apiKey(),
                settings,
                provider,
                action,
                request.recaptchaV3(),
                request.scoreThreshold(),
                "action",
                "scoreThreshold");
        validateEnabledConfiguration(
                request.loginEnabled(),
                request.siteKey(),
                request.projectId(),
                request.secretKey(),
                request.apiKey(),
                settings,
                provider,
                loginAction,
                request.loginRecaptchaV3(),
                request.loginScoreThreshold(),
                "loginAction",
                "loginScoreThreshold");
    }

    private void validateEnabledConfiguration(
            boolean enabled,
            String siteKey,
            String projectId,
            String secretKey,
            String apiKey,
            LoginSettingsEntity settings,
            String provider,
            String action,
            boolean recaptchaV3,
            double scoreThreshold,
            String actionField,
            String scoreField) {
        if (!enabled) {
            return;
        }
        if (!present(siteKey)) {
            throw invalid("siteKey", "CAPTCHA site key is required when CAPTCHA is enabled");
        }
        if ("enterprise".equals(provider)) {
            if (!present(projectId)) {
                throw invalid(
                        "projectId", "Enterprise project ID is required when CAPTCHA is enabled");
            }
            if (!present(apiKey) && !present(settings.getRegistrationCaptchaApiKeyEncrypted())) {
                throw invalid("apiKey", "Enterprise API key is required when CAPTCHA is enabled");
            }
        } else if (!present(secretKey)
                && !present(settings.getRegistrationCaptchaSecretEncrypted())) {
            throw invalid("secretKey", "reCAPTCHA secret key is required when CAPTCHA is enabled");
        }
        if ("enterprise".equals(provider) || recaptchaV3) {
            if (!validAction(action)) {
                throw invalid(actionField, "CAPTCHA action is invalid");
            }
            if (!validScoreThreshold(scoreThreshold)) {
                throw invalid(scoreField, "CAPTCHA score threshold must be between 0 and 1");
            }
        }
    }

    private RegistrationCaptchaConfiguration configuration(
            LoginSettingsEntity settings, String secretKey, String apiKey) {
        return configuration(
                settings,
                secretKey,
                apiKey,
                settings.isRegistrationCaptchaEnabled(),
                action(settings.getRegistrationCaptchaAction(), "register"),
                settings.isRegistrationCaptchaV3(),
                settings.getRegistrationCaptchaScoreThreshold());
    }

    private RegistrationCaptchaConfiguration configuration(
            LoginSettingsEntity settings,
            String secretKey,
            String apiKey,
            boolean enabled,
            String action,
            boolean recaptchaV3,
            double scoreThreshold) {
        return new RegistrationCaptchaConfiguration(
                enabled,
                provider(settings),
                value(settings.getRegistrationCaptchaSiteKey()),
                secretKey,
                value(settings.getRegistrationCaptchaProjectId()),
                apiKey,
                action,
                recaptchaV3,
                scoreThreshold,
                settings.isRegistrationCaptchaUseRecaptchaNet());
    }

    private String encrypt(String value, String field) {
        try {
            return secretCipher.encrypt(value.trim());
        } catch (IllegalStateException exception) {
            throw ApiException.badRequest(
                    field, ApiErrorCode.INVALID_REQUEST, exception.getMessage());
        }
    }

    private String decrypt(String encrypted) {
        if (!present(encrypted)) {
            return "";
        }
        try {
            return secretCipher.decrypt(encrypted);
        } catch (IllegalStateException exception) {
            log.warn("Registration CAPTCHA secret could not be decrypted", exception);
            return "";
        }
    }

    private LoginSettingsEntity settings() {
        return repository
                .findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Login settings are not initialized"));
    }

    private static ApiException invalid(String field, String message) {
        return ApiException.badRequest(field, ApiErrorCode.INVALID_REQUEST, message);
    }

    private static String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeAction(String action, String fallback) {
        return present(action) ? action.trim() : fallback;
    }

    private static String provider(LoginSettingsEntity settings) {
        String provider = value(settings.getRegistrationCaptchaProvider()).toLowerCase(Locale.ROOT);
        return PROVIDERS.contains(provider) ? provider : "recaptcha";
    }

    private static String action(String action, String fallback) {
        String value = value(action);
        return validAction(value) ? value : fallback;
    }

    private static boolean validAction(String action) {
        return action != null && ACTION_PATTERN.matcher(action).matches();
    }

    private static boolean validScoreThreshold(double scoreThreshold) {
        return scoreThreshold >= 0.0 && scoreThreshold <= 1.0;
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimToNull(String value) {
        return present(value) ? value.trim() : null;
    }
}
