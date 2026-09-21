package io.github.susimsek.springauthserversamples.service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.security.SocialLoginProperties;
import io.github.susimsek.springauthserversamples.config.security.SocialLoginSecretCipher;
import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRegistrationCaptchaRequestDTO;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegistrationCaptchaSettingsServiceTest {

    @Test
    void encryptsSecretsAndDoesNotExposeThemInAdminOrPublicSettings() {
        LoginSettingsEntity settings = new LoginSettingsEntity();
        settings.setId(1L);
        settings.setRegistrationCaptchaProvider("recaptcha");
        settings.setRegistrationCaptchaAction("register");
        settings.setRegistrationCaptchaScoreThreshold(0.7);
        LoginSettingsRepository repository = mock(LoginSettingsRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(settings));
        when(repository.save(settings)).thenReturn(settings);

        SocialLoginProperties properties = new SocialLoginProperties();
        properties.setEncryptionKey("test-encryption-key");
        RegistrationCaptchaSettingsService service =
                new RegistrationCaptchaSettingsService(
                        repository,
                        new SocialLoginSecretCipher(properties),
                        mock(AdminAuditEventService.class));

        service.update(
                new AdminRegistrationCaptchaRequestDTO(
                        true,
                        "recaptcha",
                        "site-key",
                        "secret-value",
                        "",
                        "",
                        "register",
                        false,
                        0.7,
                        false,
                        false,
                        "login",
                        false,
                        0.7));

        assertThat(settings.getRegistrationCaptchaSecretEncrypted())
                .startsWith("v1:")
                .doesNotContain("secret-value");
        assertThat(service.adminSettings().secretConfigured()).isTrue();
        assertThat(service.adminSettings().siteKey()).isEqualTo("site-key");
        assertThat(service.publicConfiguration().secretKey()).isEqualTo("configured");
        assertThat(service.verificationConfiguration().secretKey()).isEqualTo("secret-value");
    }

    @Test
    void exposesLoginAndVerificationConfigurationsWithFallbacks() {
        LoginSettingsEntity settings = settings();
        settings.setRegistrationCaptchaProvider("unknown");
        settings.setRegistrationCaptchaAction("invalid action");
        settings.setLoginCaptchaAction(" ");
        settings.setRegistrationCaptchaSecretEncrypted("encrypted-secret");
        settings.setRegistrationCaptchaApiKeyEncrypted("encrypted-api-key");
        LoginSettingsRepository repository = mock(LoginSettingsRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(settings));
        SocialLoginSecretCipher cipher = mock(SocialLoginSecretCipher.class);
        when(cipher.decrypt("encrypted-secret")).thenReturn("secret");
        when(cipher.decrypt("encrypted-api-key")).thenReturn("api-key");
        RegistrationCaptchaSettingsService service = service(repository, cipher);

        assertThat(service.adminSettings().provider()).isEqualTo("recaptcha");
        assertThat(service.adminSettings().action()).isEqualTo("register");
        assertThat(service.adminSettings().loginAction()).isEqualTo("login");
        assertThat(service.publicConfiguration().secretKey()).isEqualTo("configured");
        assertThat(service.publicConfiguration().apiKey()).isEqualTo("configured");
        assertThat(service.loginPublicConfiguration().action()).isEqualTo("login");
        assertThat(service.verificationConfiguration().secretKey()).isEqualTo("secret");
        assertThat(service.verificationConfiguration().apiKey()).isEqualTo("api-key");
        assertThat(service.loginVerificationConfiguration().apiKey()).isEqualTo("api-key");
    }

    @Test
    void preservesExistingSecretsAndNormalizesDisabledConfiguration() {
        LoginSettingsEntity settings = settings();
        settings.setRegistrationCaptchaSecretEncrypted("old-secret");
        settings.setRegistrationCaptchaApiKeyEncrypted("old-api");
        LoginSettingsRepository repository = mock(LoginSettingsRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(settings));
        when(repository.save(settings)).thenReturn(settings);
        SocialLoginSecretCipher cipher = mock(SocialLoginSecretCipher.class);
        RegistrationCaptchaSettingsService service = service(repository, cipher);

        service.update(
                new AdminRegistrationCaptchaRequestDTO(
                        false,
                        " RECAPTCHA ",
                        " site-key ",
                        " ",
                        "",
                        "",
                        " ",
                        false,
                        0.5,
                        true,
                        false,
                        " ",
                        false,
                        0.5));

        assertThat(settings.getRegistrationCaptchaProvider()).isEqualTo("recaptcha");
        assertThat(settings.getRegistrationCaptchaSiteKey()).isEqualTo("site-key");
        assertThat(settings.getRegistrationCaptchaAction()).isEqualTo("register");
        assertThat(settings.getRegistrationCaptchaSecretEncrypted()).isEqualTo("old-secret");
        assertThat(settings.getRegistrationCaptchaApiKeyEncrypted()).isEqualTo("old-api");
    }

    @Test
    void rejectsInvalidProviderAndEnabledRecaptchaConfiguration() {
        LoginSettingsRepository repository = mock(LoginSettingsRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(settings()));
        RegistrationCaptchaSettingsService service =
                service(repository, mock(SocialLoginSecretCipher.class));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        request(
                                                "other",
                                                true,
                                                "site",
                                                "secret",
                                                "register",
                                                false,
                                                0.5,
                                                false,
                                                false,
                                                "login",
                                                false,
                                                0.5)))
                .isInstanceOf(ApiException.class)
                .hasMessage("CAPTCHA provider is invalid");
        assertThatThrownBy(
                        () ->
                                service.update(
                                        request(
                                                "recaptcha",
                                                true,
                                                "",
                                                "secret",
                                                "register",
                                                false,
                                                0.5,
                                                false,
                                                false,
                                                "login",
                                                false,
                                                0.5)))
                .isInstanceOf(ApiException.class)
                .hasMessage("CAPTCHA site key is required when CAPTCHA is enabled");
        assertThatThrownBy(
                        () ->
                                service.update(
                                        request(
                                                "recaptcha",
                                                true,
                                                "site",
                                                "",
                                                "register",
                                                false,
                                                0.5,
                                                false,
                                                false,
                                                "login",
                                                false,
                                                0.5)))
                .isInstanceOf(ApiException.class)
                .hasMessage("reCAPTCHA secret key is required when CAPTCHA is enabled");
        assertThatThrownBy(
                        () ->
                                service.update(
                                        request(
                                                "recaptcha",
                                                true,
                                                "site",
                                                "secret",
                                                "bad action",
                                                true,
                                                1.5,
                                                false,
                                                false,
                                                "login",
                                                false,
                                                0.5)))
                .isInstanceOf(ApiException.class)
                .hasMessage("CAPTCHA action is invalid");
    }

    @Test
    void rejectsEnterpriseAndLoginConfigurationErrors() {
        LoginSettingsRepository repository = mock(LoginSettingsRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(settings()));
        RegistrationCaptchaSettingsService service =
                service(repository, mock(SocialLoginSecretCipher.class));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        request(
                                                "enterprise",
                                                true,
                                                "site",
                                                "",
                                                "register",
                                                false,
                                                0.5,
                                                false,
                                                false,
                                                "login",
                                                false,
                                                0.5)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Enterprise project ID is required when CAPTCHA is enabled");
        assertThatThrownBy(
                        () ->
                                service.update(
                                        request(
                                                "enterprise",
                                                true,
                                                "site",
                                                "",
                                                "register",
                                                false,
                                                0.5,
                                                false,
                                                false,
                                                "login",
                                                false,
                                                0.5,
                                                "project")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Enterprise API key is required when CAPTCHA is enabled");
        assertThatThrownBy(
                        () ->
                                service.update(
                                        request(
                                                "recaptcha",
                                                false,
                                                "",
                                                "",
                                                "register",
                                                false,
                                                0.5,
                                                false,
                                                true,
                                                "bad action",
                                                true,
                                                -0.1)))
                .isInstanceOf(ApiException.class)
                .hasMessage("CAPTCHA site key is required when CAPTCHA is enabled");
    }

    @Test
    void handlesCaptchaSecretEncryptionAndDecryptionFailures() {
        LoginSettingsRepository repository = mock(LoginSettingsRepository.class);
        LoginSettingsEntity settings = settings();
        when(repository.findById(1L)).thenReturn(Optional.of(settings));
        SocialLoginSecretCipher cipher = mock(SocialLoginSecretCipher.class);
        when(cipher.encrypt("secret"))
                .thenThrow(new IllegalStateException("encryption unavailable"));
        when(cipher.decrypt("encrypted"))
                .thenThrow(new IllegalStateException("decryption unavailable"));
        RegistrationCaptchaSettingsService service = service(repository, cipher);

        assertThatThrownBy(
                        () ->
                                service.update(
                                        request(
                                                "recaptcha",
                                                true,
                                                "site",
                                                "secret",
                                                "register",
                                                false,
                                                0.5,
                                                false,
                                                false,
                                                "login",
                                                false,
                                                0.5)))
                .isInstanceOf(ApiException.class);

        settings.setRegistrationCaptchaSecretEncrypted("encrypted");
        assertThat(service.verificationConfiguration().secretKey()).isEmpty();
    }

    private static LoginSettingsEntity settings() {
        LoginSettingsEntity settings = new LoginSettingsEntity();
        settings.setRegistrationCaptchaProvider("recaptcha");
        settings.setRegistrationCaptchaAction("register");
        settings.setRegistrationCaptchaScoreThreshold(0.7);
        settings.setLoginCaptchaAction("login");
        settings.setLoginCaptchaScoreThreshold(0.7);
        settings.setRegistrationCaptchaEnabled(true);
        settings.setLoginCaptchaEnabled(true);
        settings.setRegistrationCaptchaSiteKey("site");
        return settings;
    }

    private static RegistrationCaptchaSettingsService service(
            LoginSettingsRepository repository, SocialLoginSecretCipher cipher) {
        return new RegistrationCaptchaSettingsService(
                repository, cipher, mock(AdminAuditEventService.class));
    }

    private static AdminRegistrationCaptchaRequestDTO request(
            String provider,
            boolean enabled,
            String siteKey,
            String secretKey,
            String action,
            boolean recaptchaV3,
            double scoreThreshold,
            boolean useRecaptchaNet,
            boolean loginEnabled,
            String loginAction,
            boolean loginRecaptchaV3,
            double loginScoreThreshold) {
        return request(
                provider,
                enabled,
                siteKey,
                secretKey,
                action,
                recaptchaV3,
                scoreThreshold,
                useRecaptchaNet,
                loginEnabled,
                loginAction,
                loginRecaptchaV3,
                loginScoreThreshold,
                "");
    }

    private static AdminRegistrationCaptchaRequestDTO request(
            String provider,
            boolean enabled,
            String siteKey,
            String secretKey,
            String action,
            boolean recaptchaV3,
            double scoreThreshold,
            boolean useRecaptchaNet,
            boolean loginEnabled,
            String loginAction,
            boolean loginRecaptchaV3,
            double loginScoreThreshold,
            String projectId) {
        return new AdminRegistrationCaptchaRequestDTO(
                enabled,
                provider,
                siteKey,
                secretKey,
                projectId,
                "",
                action,
                recaptchaV3,
                scoreThreshold,
                useRecaptchaNet,
                loginEnabled,
                loginAction,
                loginRecaptchaV3,
                loginScoreThreshold);
    }
}
