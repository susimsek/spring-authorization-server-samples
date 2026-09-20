package io.github.susimsek.springauthserversamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.security.ReloadableClientRegistrationRepository;
import io.github.susimsek.springauthserversamples.config.security.SocialLoginProperties;
import io.github.susimsek.springauthserversamples.config.security.SocialLoginSecretCipher;
import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSocialProviderRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSocialProvidersRequestDTO;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.repository.SocialIdentityRepository;
import io.github.susimsek.springauthserversamples.repository.SocialProviderRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class SocialProviderSettingsServiceTest {

    private final LoginSettingsRepository repository = mock(LoginSettingsRepository.class);
    private final SocialIdentityRepository identityRepository =
            mock(SocialIdentityRepository.class);
    private final SocialProviderRepository providerRepository =
            mock(SocialProviderRepository.class);
    private final SocialLoginProperties properties = new SocialLoginProperties();
    private final SocialLoginSecretCipher secretCipher = mock(SocialLoginSecretCipher.class);
    private final ObjectProvider<ReloadableClientRegistrationRepository> registrations =
            mock(ObjectProvider.class);
    private final AdminAuditEventService auditEventService = mock(AdminAuditEventService.class);
    private LoginSettingsEntity settings;
    private SocialProviderSettingsService service;

    @BeforeEach
    void setUp() {
        settings = new LoginSettingsEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(settings));
        when(providerRepository.findAll()).thenReturn(List.of());
        when(registrations.getIfAvailable()).thenReturn(null);
        service =
                new SocialProviderSettingsService(
                        repository,
                        identityRepository,
                        providerRepository,
                        properties,
                        secretCipher,
                        registrations,
                        auditEventService);
    }

    @Test
    void exposesProviderDefaultsAndLookupModes() {
        properties.setEnabled(true);
        properties.google().setClientId("google-id");
        properties.google().setClientSecret("google-secret");

        assertThat(service.isEnabled()).isTrue();
        assertThat(service.adminSettings()).hasSize(4);
        assertThat(service.configuredProviders())
                .singleElement()
                .satisfies(
                        provider -> {
                            assertThat(provider.registrationId()).isEqualTo("google");
                            assertThat(provider)
                                    .extracting(
                                            SocialProviderSettingsService.ProviderCredentials
                                                    ::clientId)
                                    .isEqualTo("google-id");
                        });
        assertThat(service.provider("google")).isNotNull();
        assertThat(service.provider("missing")).isNull();
        assertThat(service.provider(null)).isNull();
        assertThat(service.syncMode("missing")).isNull();
        verify(providerRepository).findByRegistrationId("missing");
    }

    @Test
    void updatesAllBuiltInProvidersAndClearsStoredTokens() {
        AdminSocialProviderRequestDTO google = request("google", "google");
        AdminSocialProviderRequestDTO github = request("github", "github");
        AdminSocialProviderRequestDTO linkedin = request("linkedin", "linkedin");
        AdminSocialProviderRequestDTO microsoft = request("microsoft", "microsoft");

        assertThat(
                        service.update(
                                new AdminSocialProvidersRequestDTO(
                                        List.of(google, github, linkedin, microsoft))))
                .hasSize(4);
        verify(repository).save(settings);
        verify(identityRepository).findAllByProvider("google");
        verify(identityRepository).findAllByProvider("microsoft");
        verify(auditEventService).record("social.providers.updated", "social-providers", "default");
    }

    @Test
    void rejectsDuplicateAndUnreadableStoredTokenSettings() {
        assertThatThrownBy(
                        () ->
                                service.update(
                                        new AdminSocialProvidersRequestDTO(
                                                List.of(
                                                        request("google", "same"),
                                                        request("github", "same")))))
                .isInstanceOf(RuntimeException.class);

        AdminSocialProviderRequestDTO invalid =
                new AdminSocialProviderRequestDTO(
                        "google", "google", "id", null, false, false, false, false, "sub", false,
                        true, 0, "always");
        assertThatThrownBy(
                        () -> service.update(new AdminSocialProvidersRequestDTO(List.of(invalid))))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void refreshesConfiguredClientRegistrationsWhenAvailable() {
        ReloadableClientRegistrationRepository reloadable =
                mock(ReloadableClientRegistrationRepository.class);
        when(registrations.getIfAvailable()).thenReturn(reloadable);
        properties.google().setClientId("id");
        properties.google().setClientSecret("secret");

        service.refreshClientRegistrations();

        verify(reloadable).replace(any());
    }

    private static AdminSocialProviderRequestDTO request(String provider, String alias) {
        return new AdminSocialProviderRequestDTO(
                provider,
                alias,
                provider + "-client",
                provider + "-secret",
                true,
                false,
                true,
                false,
                "sub,email",
                false,
                false,
                1,
                "always");
    }
}
