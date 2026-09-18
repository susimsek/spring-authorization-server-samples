package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.LocalizationMessageOverrideEntity;
import io.github.susimsek.springauthserversamples.domain.LocalizationSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.admin.LocalizationSettingsRequestDTO;
import io.github.susimsek.springauthserversamples.repository.LocalizationMessageOverrideRepository;
import io.github.susimsek.springauthserversamples.repository.LocalizationSettingsRepository;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class LocalizationSettingsServiceTest {

    @Mock private LocalizationSettingsRepository settingsRepository;
    @Mock private LocalizationMessageOverrideRepository overrideRepository;
    @Mock private AdminAuditEventService auditEventService;

    @Test
    void rejectsDefaultLocaleThatIsNotEnabled() {
        assertThatThrownBy(
                        () ->
                                service()
                                        .update(
                                                new LocalizationSettingsRequestDTO(
                                                        true, "tr", List.of("en"))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void exposesConfiguredLocalesAndDefault() {
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings()));

        var result = service().get();

        assertThat(result.defaultLocale()).isEqualTo("en");
        assertThat(result.supportedLocales()).containsExactly("en", "tr");
        assertThat(result.availableLocales()).containsExactly("en", "tr");
        assertThat(result.availableBundles())
                .containsExactly("login", "account", "admin", "email", "backend");
    }

    @Test
    void returnsOverridesForSelectedBundle() {
        LocalizationMessageOverrideEntity override = new LocalizationMessageOverrideEntity();
        override.setLocale("tr");
        override.setBundle("backend");
        override.setMessageKey("login.title");
        override.setMessageValue("Oturum aç");
        when(overrideRepository.findByLocaleAndBundle(
                        "tr",
                        "backend",
                        PageRequest.of(
                                0,
                                1000,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.ASC,
                                        "messageKey"))))
                .thenReturn(new PageImpl<>(List.of(override)));

        var result = service().publicOverrides("tr", "backend");

        assertThat(result).containsEntry("login.title", "Oturum aç");
    }

    @Test
    void exposesBundledBackendMessagesForEffectiveMessageSearch() {
        var result = service().bundledMessages("en", "backend");

        assertThat(result)
                .containsEntry("app.security.unauthorized", "Authentication is required.");
        assertThat(result).doesNotContainKey("mail.test.subject");
    }

    @Test
    void exposesBundledEmailMessagesForEffectiveMessageSearch() {
        var result = service().bundledMessages("en", "email");

        assertThat(result).containsEntry("mail.test.subject", "SMTP connection test");
        assertThat(result).doesNotContainKey("app.security.unauthorized");
    }

    private LocalizationSettingsService service() {
        return new LocalizationSettingsService(
                settingsRepository, overrideRepository, auditEventService);
    }

    private static LocalizationSettingsEntity settings() {
        LocalizationSettingsEntity settings = new LocalizationSettingsEntity();
        settings.setId(1L);
        settings.setInternationalizationEnabled(true);
        settings.setDefaultLocale("en");
        settings.setSupportedLocales("en,tr");
        return settings;
    }
}
