package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.config.security.SocialLoginProperties;
import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.SocialIdentityEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.account.SocialLinkDTO;
import io.github.susimsek.springauthserversamples.dto.account.SocialProviderDTO;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.SocialIdentityRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.security.AuthoritiesConstants;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private static final Set<String> SUPPORTED_PROVIDERS =
            Set.of("google", "github", "linkedin", "microsoft");

    public static final String PENDING_SOCIAL_LINK = "socialLogin.pendingLink";
    public static final String PENDING_SOCIAL_LINK_TARGET = "socialLogin.pendingLinkTarget";

    private final UserRepository userRepository;
    private final SocialIdentityRepository socialIdentityRepository;
    private final AuthorityRepository authorityRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final LoginSettingsService loginSettingsService;
    private final SocialProviderSettingsService socialProviderSettingsService;
    private final UserAccessInvalidationService userAccessInvalidationService;
    private final AdminAuditEventService auditEventService;

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public String findOrCreate(OAuth2AuthenticationToken authentication) {
        String provider =
                authentication.getAuthorizedClientRegistrationId().toLowerCase(Locale.ROOT);
        ensureProviderEnabled(provider);
        Map<String, Object> attributes = authentication.getPrincipal().getAttributes();
        String subject = requiredAttribute(attributes, "sub", "id");
        SocialIdentityEntity existing =
                socialIdentityRepository.findByProviderAndSubject(provider, subject).orElse(null);
        if (existing != null) {
            return existing.getUser().getUsername();
        }

        String email = normalizeEmail(attribute(attributes, "email"));
        if (email != null && userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new SocialAccountLinkRequiredException(provider, subject, email);
        }

        AuthorityEntity userAuthority =
                authorityRepository
                        .findByName(AuthoritiesConstants.USER)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "The default user authority is not configured"));
        UserEntity user = new UserEntity();
        user.setUsername(username(provider, subject));
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setFirstName(firstName(attributes));
        user.setLastName(lastName(attributes));
        user.setEmail(email);
        user.setEmailVerified(email != null);
        user.setEnabled(true);
        user.setPasswordChangedAt(Instant.now());
        user.setMustChangePassword(false);
        user.setTemporaryPassword(false);
        user.setAuthorities(java.util.Set.of(userAuthority));
        UserEntity saved = userRepository.save(user);
        socialIdentityRepository.save(new SocialIdentityEntity(provider, subject, saved));
        return saved.getUsername();
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public String linkExisting(
            String username, String expectedProvider, OAuth2AuthenticationToken authentication) {
        String provider = provider(authentication);
        ensureProviderEnabled(provider);
        if (!expectedProvider.equals(provider)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("social_provider_mismatch"),
                    "The social provider did not match the pending account link");
        }
        String subject = subject(authentication.getPrincipal().getAttributes());
        link(username, provider, subject);
        return username;
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void linkPending(String username, Map<?, ?> pendingLink) {
        String provider = pendingValue(pendingLink, "provider");
        String subject = pendingValue(pendingLink, "subject");
        if (provider == null || subject == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_pending_account_link"),
                    "The pending social account link is invalid");
        }
        ensureProviderEnabled(provider);
        link(username, provider, subject);
    }

    public Set<String> linkedProviders(String username) {
        return socialIdentityRepository.findAllByUserUsername(username).stream()
                .map(SocialIdentityEntity::getProvider)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void unlink(String username, String provider) {
        String normalizedProvider = normalizeProvider(provider);
        List<SocialIdentityEntity> identities =
                socialIdentityRepository.findAllByUserUsernameAndProvider(
                        username, normalizedProvider);
        if (identities.isEmpty()) {
            throw ApiException.notFound("Social account link not found");
        }
        socialIdentityRepository.deleteAll(identities);
        userAccessInvalidationService.invalidate(username);
        auditEventService.record(
                "account.social_link.deleted",
                "social_identity",
                username + ":" + normalizedProvider);
    }

    public List<String> configuredProviders(SocialLoginProperties properties) {
        return java.util.stream.Stream.of(
                        new ProviderConfiguration("google", properties.google()),
                        new ProviderConfiguration("github", properties.github()),
                        new ProviderConfiguration("linkedin", properties.linkedin()),
                        new ProviderConfiguration("microsoft", properties.microsoft()))
                .filter(
                        configuration ->
                                configuration.provider().configured()
                                        && isProviderEnabled(configuration.registrationId()))
                .map(ProviderConfiguration::registrationId)
                .sorted()
                .toList();
    }

    public List<String> configuredProviders() {
        return socialProviderSettingsService.configuredProviders().stream()
                .map(SocialProviderSettingsService.ProviderCredentials::registrationId)
                .filter(this::isProviderEnabled)
                .toList();
    }

    public List<SocialProviderDTO> availableProviders() {
        return socialProviderSettingsService.effectiveProviders().stream()
                .filter(provider -> isProviderEnabled(provider.registrationId()))
                .map(
                        provider ->
                                new SocialProviderDTO(
                                        provider.registrationId(), provider.configured()))
                .toList();
    }

    public List<SocialLinkDTO> socialLinks(String username) {
        Set<String> linked = linkedProviders(username);
        return socialProviderSettingsService.effectiveProviders().stream()
                .filter(
                        provider ->
                                isProviderEnabled(provider.registrationId())
                                        || linked.contains(provider.registrationId()))
                .map(
                        provider ->
                                new SocialLinkDTO(
                                        provider.registrationId(),
                                        displayName(provider.registrationId()),
                                        linked.contains(provider.registrationId()),
                                        provider.configured(),
                                        isProviderEnabled(provider.registrationId())))
                .toList();
    }

    public boolean isProviderEnabled(String provider) {
        return provider != null && loginSettingsService.isSocialProviderEnabled(provider);
    }

    private void ensureProviderEnabled(String provider) {
        if (!isProviderEnabled(provider)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("social_provider_disabled"),
                    "This social login provider is disabled");
        }
    }

    private static String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_PROVIDERS.contains(normalized)) {
            throw ApiException.badRequest(
                    "provider", ApiErrorCode.INVALID_REQUEST, "The social provider is invalid");
        }
        return normalized;
    }

    private static String displayName(String provider) {
        return switch (provider) {
            case "github" -> "GitHub";
            case "linkedin" -> "LinkedIn";
            case "microsoft" -> "Microsoft";
            default -> "Google";
        };
    }

    private record ProviderConfiguration(
            String registrationId, SocialLoginProperties.Provider provider) {}

    private void link(String username, String provider, String subject) {
        UserEntity user =
                userRepository
                        .findForLoginUpdate(username)
                        .orElseThrow(
                                () ->
                                        new OAuth2AuthenticationException(
                                                new OAuth2Error("user_not_found"),
                                                "The local account could not be found"));
        SocialIdentityEntity existing =
                socialIdentityRepository.findByProviderAndSubject(provider, subject).orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.getUser().getId(), user.getId())) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("social_identity_already_linked"),
                        "This social account is already linked to another local account");
            }
            return;
        }
        socialIdentityRepository.save(new SocialIdentityEntity(provider, subject, user));
    }

    private static String requiredAttribute(Map<String, Object> attributes, String... names) {
        for (String name : names) {
            String value = attribute(attributes, name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_user_info"),
                "The social provider did not return a subject");
    }

    private static String provider(OAuth2AuthenticationToken authentication) {
        return authentication.getAuthorizedClientRegistrationId().toLowerCase(Locale.ROOT);
    }

    private static String subject(Map<String, Object> attributes) {
        return requiredAttribute(attributes, "sub", "id");
    }

    private static String pendingValue(Map<?, ?> pendingLink, String key) {
        Object value = pendingLink.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private static String attribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        return value == null ? null : String.valueOf(value).trim();
    }

    private static String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.toLowerCase(Locale.ROOT);
    }

    private static String firstName(Map<String, Object> attributes) {
        String value = firstNonBlank(attributes, "given_name", "localizedFirstName", "first_name");
        return value == null ? firstNonBlank(attributes, "name") : value;
    }

    private static String lastName(Map<String, Object> attributes) {
        return firstNonBlank(attributes, "family_name", "localizedLastName", "last_name");
    }

    private static String firstNonBlank(Map<String, Object> attributes, String... names) {
        for (String name : names) {
            String value = attribute(attributes, name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String username(String provider, String subject) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest((provider + ":" + subject).getBytes(StandardCharsets.UTF_8));
            return "social_" + provider + "_" + HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
