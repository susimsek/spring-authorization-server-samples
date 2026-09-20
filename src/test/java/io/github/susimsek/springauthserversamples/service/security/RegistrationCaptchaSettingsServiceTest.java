package io.github.susimsek.springauthserversamples.service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.security.SocialLoginProperties;
import io.github.susimsek.springauthserversamples.config.security.SocialLoginSecretCipher;
import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRegistrationCaptchaRequestDTO;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
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
}
