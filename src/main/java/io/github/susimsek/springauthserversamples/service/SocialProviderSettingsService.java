package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.config.security.ReloadableClientRegistrationRepository;
import io.github.susimsek.springauthserversamples.config.security.SocialLoginProperties;
import io.github.susimsek.springauthserversamples.config.security.SocialLoginSecretCipher;
import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSocialProviderDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSocialProviderRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSocialProvidersRequestDTO;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialProviderSettingsService {

    private static final long SETTINGS_ID = 1L;
    private static final Set<String> SUPPORTED_PROVIDERS =
            Set.of("google", "github", "linkedin", "microsoft");

    private final LoginSettingsRepository repository;
    private final SocialLoginProperties properties;
    private final SocialLoginSecretCipher secretCipher;
    private final ObjectProvider<ReloadableClientRegistrationRepository>
            clientRegistrationRepository;
    private final AdminAuditEventService auditEventService;

    @Transactional(readOnly = true)
    public List<AdminSocialProviderDTO> adminSettings() {
        LoginSettingsEntity settings = settings();
        return effectiveProviders(settings).map(this::toAdminDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderCredentials> configuredProviders() {
        return effectiveProviders(settings()).filter(ProviderCredentials::configured).toList();
    }

    @Transactional
    public List<AdminSocialProviderDTO> update(AdminSocialProvidersRequestDTO request) {
        LoginSettingsEntity settings = settings();
        Set<String> seen = new java.util.HashSet<>();
        for (AdminSocialProviderRequestDTO provider : request.providers()) {
            String registrationId = normalizeProvider(provider.provider());
            if (!SUPPORTED_PROVIDERS.contains(registrationId) || !seen.add(registrationId)) {
                throw ApiException.badRequest(
                        "provider", ApiErrorCode.INVALID_REQUEST, "The social provider is invalid");
            }
            apply(registrationId, settings, provider.clientId().trim(), provider.clientSecret());
        }
        repository.save(settings);
        refreshClientRegistrations();
        auditEventService.record("social.providers.updated", "social-providers", "default");
        return adminSettings();
    }

    public List<ProviderCredentials> effectiveProviders() {
        return effectiveProviders(settings()).toList();
    }

    private java.util.stream.Stream<ProviderCredentials> effectiveProviders(
            LoginSettingsEntity settings) {
        return Arrays.stream(new String[] {"google", "github", "linkedin", "microsoft"})
                .map(provider -> credentials(provider, settings));
    }

    private ProviderCredentials credentials(String provider, LoginSettingsEntity settings) {
        SocialLoginProperties.Provider configured = configuredProperties(provider);
        String clientId = firstNonBlank(storedClientId(provider, settings), configured.clientId());
        String clientSecret = storedSecret(provider, settings);
        if (clientSecret.isBlank()) {
            clientSecret = configured.clientSecret();
        }
        return new ProviderCredentials(provider, clientId, clientSecret);
    }

    private AdminSocialProviderDTO toAdminDTO(ProviderCredentials credentials) {
        return new AdminSocialProviderDTO(
                credentials.registrationId(), credentials.clientId(), credentials.configured());
    }

    private void apply(
            String provider, LoginSettingsEntity settings, String clientId, String clientSecret) {
        if (clientSecret != null && !clientSecret.isBlank()) {
            try {
                setSecret(provider, settings, secretCipher.encrypt(clientSecret.trim()));
            } catch (IllegalStateException exception) {
                throw ApiException.badRequest(
                        "clientSecret", ApiErrorCode.INVALID_REQUEST, exception.getMessage());
            }
        }
        setClientId(provider, settings, clientId);
    }

    private void refreshClientRegistrations() {
        ReloadableClientRegistrationRepository repository =
                clientRegistrationRepository.getIfAvailable();
        if (repository != null) {
            repository.replace(
                    io.github.susimsek.springauthserversamples.config.security.SocialLoginConfig
                            .registrations(configuredProviders()));
        }
    }

    private SocialLoginProperties.Provider configuredProperties(String provider) {
        return switch (provider) {
            case "google" -> properties.google();
            case "github" -> properties.github();
            case "linkedin" -> properties.linkedin();
            case "microsoft" -> properties.microsoft();
            default -> throw new IllegalArgumentException("Unsupported social provider");
        };
    }

    private String storedClientId(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.getGoogleClientId();
            case "github" -> settings.getGithubClientId();
            case "linkedin" -> settings.getLinkedinClientId();
            case "microsoft" -> settings.getMicrosoftClientId();
            default -> "";
        };
    }

    private String storedSecret(String provider, LoginSettingsEntity settings) {
        String encrypted =
                switch (provider) {
                    case "google" -> settings.getGoogleClientSecretEncrypted();
                    case "github" -> settings.getGithubClientSecretEncrypted();
                    case "linkedin" -> settings.getLinkedinClientSecretEncrypted();
                    case "microsoft" -> settings.getMicrosoftClientSecretEncrypted();
                    default -> "";
                };
        return encrypted == null || encrypted.isBlank() ? "" : secretCipher.decrypt(encrypted);
    }

    private void setClientId(String provider, LoginSettingsEntity settings, String value) {
        consumer(
                        provider,
                        settings::setGoogleClientId,
                        settings::setGithubClientId,
                        settings::setLinkedinClientId,
                        settings::setMicrosoftClientId)
                .accept(value);
    }

    private void setSecret(String provider, LoginSettingsEntity settings, String value) {
        consumer(
                        provider,
                        settings::setGoogleClientSecretEncrypted,
                        settings::setGithubClientSecretEncrypted,
                        settings::setLinkedinClientSecretEncrypted,
                        settings::setMicrosoftClientSecretEncrypted)
                .accept(value);
    }

    private static Consumer<String> consumer(
            String provider,
            Consumer<String> google,
            Consumer<String> github,
            Consumer<String> linkedin,
            Consumer<String> microsoft) {
        return switch (provider) {
            case "google" -> google;
            case "github" -> github;
            case "linkedin" -> linkedin;
            case "microsoft" -> microsoft;
            default -> value -> {};
        };
    }

    private static String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? (fallback == null ? "" : fallback) : first;
    }

    private LoginSettingsEntity settings() {
        return repository
                .findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Login settings are not configured"));
    }

    public record ProviderCredentials(String registrationId, String clientId, String clientSecret) {
        public boolean configured() {
            return clientId != null
                    && !clientId.isBlank()
                    && clientSecret != null
                    && !clientSecret.isBlank();
        }
    }
}
