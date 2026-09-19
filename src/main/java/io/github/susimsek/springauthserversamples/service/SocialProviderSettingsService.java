package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.config.security.ReloadableClientRegistrationRepository;
import io.github.susimsek.springauthserversamples.config.security.SocialLoginProperties;
import io.github.susimsek.springauthserversamples.config.security.SocialLoginSecretCipher;
import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.domain.SocialIdentityEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSocialProviderDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSocialProviderRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSocialProvidersRequestDTO;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.repository.SocialIdentityRepository;
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
    private final SocialIdentityRepository socialIdentityRepository;
    private final SocialLoginProperties properties;
    private final SocialLoginSecretCipher secretCipher;
    private final ObjectProvider<ReloadableClientRegistrationRepository>
            clientRegistrationRepository;
    private final AdminAuditEventService auditEventService;

    @Transactional(readOnly = true)
    public List<AdminSocialProviderDTO> adminSettings() {
        LoginSettingsEntity settings = settings();
        return effectiveProviders(settings).sorted().map(this::toAdminDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderCredentials> configuredProviders() {
        return effectiveProviders(settings()).filter(ProviderCredentials::configured).toList();
    }

    public ProviderCredentials provider(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return effectiveProviders(settings())
                .filter(
                        provider ->
                                provider.registrationId().equals(value)
                                        || provider.alias().equals(value))
                .findFirst()
                .orElse(null);
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    public void refreshClientRegistrations() {
        ReloadableClientRegistrationRepository repository =
                clientRegistrationRepository.getIfAvailable();
        if (repository != null) {
            repository.replace(
                    io.github.susimsek.springauthserversamples.config.security.SocialLoginConfig
                            .registrations(configuredProviders()));
        }
    }

    @Transactional
    public List<AdminSocialProviderDTO> update(AdminSocialProvidersRequestDTO request) {
        LoginSettingsEntity settings = settings();
        Set<String> seen = new java.util.HashSet<>();
        for (AdminSocialProviderRequestDTO provider : request.providers()) {
            String registrationId = normalizeProvider(provider.provider());
            String alias = provider.alias().trim().toLowerCase(Locale.ROOT);
            boolean aliasUsedByAnotherProvider =
                    effectiveProviders(settings)
                            .anyMatch(
                                    current ->
                                            !current.registrationId().equals(registrationId)
                                                    && current.alias().equals(alias));
            boolean duplicate = !seen.add(registrationId);
            if (!duplicate && !alias.equals(registrationId)) {
                duplicate = !seen.add(alias);
            }
            if (!SUPPORTED_PROVIDERS.contains(registrationId)
                    || duplicate
                    || aliasUsedByAnotherProvider) {
                throw ApiException.badRequest(
                        "provider", ApiErrorCode.INVALID_REQUEST, "The social provider is invalid");
            }
            if (provider.storedTokensReadable() && !provider.storeTokens()) {
                throw ApiException.badRequest(
                        "storedTokensReadable",
                        ApiErrorCode.INVALID_REQUEST,
                        "Stored tokens must be enabled before they can be readable");
            }
            apply(
                    registrationId,
                    settings,
                    alias,
                    provider.clientId().trim(),
                    provider.clientSecret(),
                    provider.hideOnLogin(),
                    provider.accountLinkingOnly(),
                    provider.trustEmail(),
                    provider.mfaRequired(),
                    provider.requiredClaims(),
                    provider.storeTokens(),
                    provider.storedTokensReadable(),
                    provider.guiOrder(),
                    provider.showInAccountConsole());
            if (!provider.storeTokens()) {
                clearStoredTokens(registrationId);
            }
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
        return new ProviderCredentials(
                provider,
                firstNonBlank(storedAlias(provider, settings), provider),
                clientId,
                clientSecret,
                enabled(provider, settings),
                hideOnLogin(provider, settings),
                accountLinkingOnly(provider, settings),
                trustEmail(provider, settings),
                mfaRequired(provider, settings),
                requiredClaims(provider, settings),
                storeTokens(provider, settings),
                storedTokensReadable(provider, settings),
                guiOrder(provider, settings),
                showInAccountConsole(provider, settings));
    }

    private AdminSocialProviderDTO toAdminDTO(ProviderCredentials credentials) {
        return new AdminSocialProviderDTO(
                credentials.registrationId(),
                credentials.alias(),
                credentials.hideOnLogin(),
                credentials.accountLinkingOnly(),
                credentials.trustEmail(),
                credentials.mfaRequired(),
                credentials.requiredClaims(),
                credentials.storeTokens(),
                credentials.storedTokensReadable(),
                credentials.guiOrder(),
                credentials.showInAccountConsole(),
                credentials.clientId(),
                credentials.configured());
    }

    private void apply(
            String provider,
            LoginSettingsEntity settings,
            String alias,
            String clientId,
            String clientSecret,
            boolean hideOnLogin,
            boolean accountLinkingOnly,
            boolean trustEmail,
            boolean mfaRequired,
            String requiredClaims,
            boolean storeTokens,
            boolean storedTokensReadable,
            int guiOrder,
            String showInAccountConsole) {
        if (clientSecret != null && !clientSecret.isBlank()) {
            try {
                setSecret(provider, settings, secretCipher.encrypt(clientSecret.trim()));
            } catch (IllegalStateException exception) {
                throw ApiException.badRequest(
                        "clientSecret", ApiErrorCode.INVALID_REQUEST, exception.getMessage());
            }
        }
        setAlias(provider, settings, alias);
        setHideOnLogin(provider, settings, hideOnLogin);
        setAccountLinkingOnly(provider, settings, accountLinkingOnly);
        setTrustEmail(provider, settings, trustEmail);
        setMfaRequired(provider, settings, mfaRequired);
        setRequiredClaims(provider, settings, requiredClaims);
        setStoreTokens(provider, settings, storeTokens);
        setStoredTokensReadable(provider, settings, storedTokensReadable);
        setGuiOrder(provider, settings, guiOrder);
        setShowInAccountConsole(
                provider, settings, normalizeAccountConsoleVisibility(showInAccountConsole));
        setClientId(provider, settings, clientId);
    }

    private boolean enabled(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.isGoogleLoginEnabled();
            case "github" -> settings.isGithubLoginEnabled();
            case "linkedin" -> settings.isLinkedinLoginEnabled();
            case "microsoft" -> settings.isMicrosoftLoginEnabled();
            default -> false;
        };
    }

    private String storedAlias(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.getGoogleAlias();
            case "github" -> settings.getGithubAlias();
            case "linkedin" -> settings.getLinkedinAlias();
            case "microsoft" -> settings.getMicrosoftAlias();
            default -> "";
        };
    }

    private boolean hideOnLogin(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.isGoogleHideOnLogin();
            case "github" -> settings.isGithubHideOnLogin();
            case "linkedin" -> settings.isLinkedinHideOnLogin();
            case "microsoft" -> settings.isMicrosoftHideOnLogin();
            default -> false;
        };
    }

    private boolean accountLinkingOnly(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.isGoogleAccountLinkingOnly();
            case "github" -> settings.isGithubAccountLinkingOnly();
            case "linkedin" -> settings.isLinkedinAccountLinkingOnly();
            case "microsoft" -> settings.isMicrosoftAccountLinkingOnly();
            default -> false;
        };
    }

    private boolean trustEmail(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.isGoogleTrustEmail();
            case "github" -> settings.isGithubTrustEmail();
            case "linkedin" -> settings.isLinkedinTrustEmail();
            case "microsoft" -> settings.isMicrosoftTrustEmail();
            default -> false;
        };
    }

    private boolean mfaRequired(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.isGoogleMfaRequired();
            case "github" -> settings.isGithubMfaRequired();
            case "linkedin" -> settings.isLinkedinMfaRequired();
            case "microsoft" -> settings.isMicrosoftMfaRequired();
            default -> false;
        };
    }

    private String requiredClaims(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.getGoogleRequiredClaims();
            case "github" -> settings.getGithubRequiredClaims();
            case "linkedin" -> settings.getLinkedinRequiredClaims();
            case "microsoft" -> settings.getMicrosoftRequiredClaims();
            default -> "sub";
        };
    }

    private boolean storeTokens(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.isGoogleStoreTokens();
            case "github" -> settings.isGithubStoreTokens();
            case "linkedin" -> settings.isLinkedinStoreTokens();
            case "microsoft" -> settings.isMicrosoftStoreTokens();
            default -> false;
        };
    }

    private boolean storedTokensReadable(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.isGoogleStoredTokensReadable();
            case "github" -> settings.isGithubStoredTokensReadable();
            case "linkedin" -> settings.isLinkedinStoredTokensReadable();
            case "microsoft" -> settings.isMicrosoftStoredTokensReadable();
            default -> false;
        };
    }

    private int guiOrder(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.getGoogleGuiOrder();
            case "github" -> settings.getGithubGuiOrder();
            case "linkedin" -> settings.getLinkedinGuiOrder();
            case "microsoft" -> settings.getMicrosoftGuiOrder();
            default -> 0;
        };
    }

    private String showInAccountConsole(String provider, LoginSettingsEntity settings) {
        return switch (provider) {
            case "google" -> settings.getGoogleShowInAccountConsole();
            case "github" -> settings.getGithubShowInAccountConsole();
            case "linkedin" -> settings.getLinkedinShowInAccountConsole();
            case "microsoft" -> settings.getMicrosoftShowInAccountConsole();
            default -> "always";
        };
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

    private void setAlias(String provider, LoginSettingsEntity settings, String value) {
        switch (provider) {
            case "google" -> settings.setGoogleAlias(value);
            case "github" -> settings.setGithubAlias(value);
            case "linkedin" -> settings.setLinkedinAlias(value);
            case "microsoft" -> settings.setMicrosoftAlias(value);
            default -> {}
        }
    }

    private void setHideOnLogin(String provider, LoginSettingsEntity settings, boolean value) {
        switch (provider) {
            case "google" -> settings.setGoogleHideOnLogin(value);
            case "github" -> settings.setGithubHideOnLogin(value);
            case "linkedin" -> settings.setLinkedinHideOnLogin(value);
            case "microsoft" -> settings.setMicrosoftHideOnLogin(value);
            default -> {}
        }
    }

    private void setAccountLinkingOnly(
            String provider, LoginSettingsEntity settings, boolean value) {
        switch (provider) {
            case "google" -> settings.setGoogleAccountLinkingOnly(value);
            case "github" -> settings.setGithubAccountLinkingOnly(value);
            case "linkedin" -> settings.setLinkedinAccountLinkingOnly(value);
            case "microsoft" -> settings.setMicrosoftAccountLinkingOnly(value);
            default -> {}
        }
    }

    private void setTrustEmail(String provider, LoginSettingsEntity settings, boolean value) {
        switch (provider) {
            case "google" -> settings.setGoogleTrustEmail(value);
            case "github" -> settings.setGithubTrustEmail(value);
            case "linkedin" -> settings.setLinkedinTrustEmail(value);
            case "microsoft" -> settings.setMicrosoftTrustEmail(value);
            default -> {}
        }
    }

    private void setMfaRequired(String provider, LoginSettingsEntity settings, boolean value) {
        switch (provider) {
            case "google" -> settings.setGoogleMfaRequired(value);
            case "github" -> settings.setGithubMfaRequired(value);
            case "linkedin" -> settings.setLinkedinMfaRequired(value);
            case "microsoft" -> settings.setMicrosoftMfaRequired(value);
            default -> {}
        }
    }

    private void setRequiredClaims(String provider, LoginSettingsEntity settings, String value) {
        String normalized = normalizeRequiredClaims(value);
        switch (provider) {
            case "google" -> settings.setGoogleRequiredClaims(normalized);
            case "github" -> settings.setGithubRequiredClaims(normalized);
            case "linkedin" -> settings.setLinkedinRequiredClaims(normalized);
            case "microsoft" -> settings.setMicrosoftRequiredClaims(normalized);
            default -> {}
        }
    }

    private void setStoreTokens(String provider, LoginSettingsEntity settings, boolean value) {
        switch (provider) {
            case "google" -> settings.setGoogleStoreTokens(value);
            case "github" -> settings.setGithubStoreTokens(value);
            case "linkedin" -> settings.setLinkedinStoreTokens(value);
            case "microsoft" -> settings.setMicrosoftStoreTokens(value);
            default -> {}
        }
    }

    private void setStoredTokensReadable(
            String provider, LoginSettingsEntity settings, boolean value) {
        switch (provider) {
            case "google" -> settings.setGoogleStoredTokensReadable(value);
            case "github" -> settings.setGithubStoredTokensReadable(value);
            case "linkedin" -> settings.setLinkedinStoredTokensReadable(value);
            case "microsoft" -> settings.setMicrosoftStoredTokensReadable(value);
            default -> {}
        }
    }

    private void setGuiOrder(String provider, LoginSettingsEntity settings, int value) {
        switch (provider) {
            case "google" -> settings.setGoogleGuiOrder(value);
            case "github" -> settings.setGithubGuiOrder(value);
            case "linkedin" -> settings.setLinkedinGuiOrder(value);
            case "microsoft" -> settings.setMicrosoftGuiOrder(value);
            default -> {}
        }
    }

    private void setShowInAccountConsole(
            String provider, LoginSettingsEntity settings, String value) {
        switch (provider) {
            case "google" -> settings.setGoogleShowInAccountConsole(value);
            case "github" -> settings.setGithubShowInAccountConsole(value);
            case "linkedin" -> settings.setLinkedinShowInAccountConsole(value);
            case "microsoft" -> settings.setMicrosoftShowInAccountConsole(value);
            default -> {}
        }
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

    private static String normalizeAccountConsoleVisibility(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("always", "when-linked", "never").contains(normalized)) {
            throw ApiException.badRequest(
                    "showInAccountConsole",
                    ApiErrorCode.INVALID_REQUEST,
                    "The Account Console visibility is invalid");
        }
        return normalized;
    }

    private static String normalizeRequiredClaims(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return "sub";
        }
        String result =
                Arrays.stream(normalized.split(","))
                        .map(String::trim)
                        .filter(claim -> !claim.isBlank())
                        .distinct()
                        .peek(
                                claim -> {
                                    if (!claim.matches("[A-Za-z0-9_.-]+")) {
                                        throw ApiException.badRequest(
                                                "requiredClaims",
                                                ApiErrorCode.INVALID_REQUEST,
                                                "Provider claim names are invalid");
                                    }
                                })
                        .reduce((left, right) -> left + "," + right)
                        .orElse("sub");
        if (result.length() > 500) {
            throw ApiException.badRequest(
                    "requiredClaims",
                    ApiErrorCode.INVALID_REQUEST,
                    "Provider required claims are too long");
        }
        return result;
    }

    private static String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? (fallback == null ? "" : fallback) : first;
    }

    private LoginSettingsEntity settings() {
        return repository
                .findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Login settings are not configured"));
    }

    private void clearStoredTokens(String provider) {
        List<SocialIdentityEntity> identities =
                socialIdentityRepository.findAllByProvider(provider);
        identities.forEach(
                identity -> {
                    identity.setAccessTokenEncrypted(null);
                    identity.setRefreshTokenEncrypted(null);
                    identity.setAccessTokenExpiresAt(null);
                    identity.setTokenType(null);
                    identity.setTokenScopes(null);
                });
        if (!identities.isEmpty()) {
            socialIdentityRepository.saveAll(identities);
        }
    }

    public record ProviderCredentials(
            String registrationId,
            String alias,
            String clientId,
            String clientSecret,
            boolean enabled,
            boolean hideOnLogin,
            boolean accountLinkingOnly,
            boolean trustEmail,
            boolean mfaRequired,
            String requiredClaims,
            boolean storeTokens,
            boolean storedTokensReadable,
            int guiOrder,
            String showInAccountConsole)
            implements Comparable<ProviderCredentials> {
        public ProviderCredentials(String registrationId, String clientId, String clientSecret) {
            this(
                    registrationId,
                    registrationId,
                    clientId,
                    clientSecret,
                    true,
                    false,
                    false,
                    false,
                    false,
                    "sub",
                    false,
                    false,
                    0,
                    "always");
        }

        public ProviderCredentials(
                String registrationId,
                String alias,
                String clientId,
                String clientSecret,
                boolean enabled,
                boolean hideOnLogin,
                boolean accountLinkingOnly,
                int guiOrder,
                String showInAccountConsole) {
            this(
                    registrationId,
                    alias,
                    clientId,
                    clientSecret,
                    enabled,
                    hideOnLogin,
                    accountLinkingOnly,
                    false,
                    false,
                    "sub",
                    false,
                    false,
                    guiOrder,
                    showInAccountConsole);
        }

        public ProviderCredentials(
                String registrationId,
                String alias,
                String clientId,
                String clientSecret,
                boolean enabled,
                boolean hideOnLogin,
                boolean accountLinkingOnly,
                boolean storeTokens,
                boolean storedTokensReadable,
                int guiOrder,
                String showInAccountConsole) {
            this(
                    registrationId,
                    alias,
                    clientId,
                    clientSecret,
                    enabled,
                    hideOnLogin,
                    accountLinkingOnly,
                    false,
                    false,
                    "sub",
                    storeTokens,
                    storedTokensReadable,
                    guiOrder,
                    showInAccountConsole);
        }

        public ProviderCredentials(
                String registrationId,
                String alias,
                String clientId,
                String clientSecret,
                boolean enabled,
                boolean hideOnLogin,
                boolean accountLinkingOnly,
                boolean trustEmail,
                boolean mfaRequired,
                String requiredClaims,
                int guiOrder,
                String showInAccountConsole,
                boolean storeTokens,
                boolean storedTokensReadable) {
            this(
                    registrationId,
                    alias,
                    clientId,
                    clientSecret,
                    enabled,
                    hideOnLogin,
                    accountLinkingOnly,
                    trustEmail,
                    mfaRequired,
                    requiredClaims,
                    storeTokens,
                    storedTokensReadable,
                    guiOrder,
                    showInAccountConsole);
        }

        public boolean configured() {
            return clientId != null
                    && !clientId.isBlank()
                    && clientSecret != null
                    && !clientSecret.isBlank();
        }

        @Override
        public int compareTo(ProviderCredentials other) {
            int order = Integer.compare(guiOrder, other.guiOrder);
            return order != 0 ? order : registrationId.compareTo(other.registrationId);
        }
    }
}
