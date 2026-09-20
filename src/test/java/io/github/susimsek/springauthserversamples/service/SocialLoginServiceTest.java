package io.github.susimsek.springauthserversamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.security.SocialLoginProperties;
import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.SocialIdentityEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.SocialIdentityRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.security.AuthoritiesConstants;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    @org.junit.jupiter.api.BeforeEach
    void enableSocialProviders() {
        lenient().when(loginSettingsService.isSocialProviderEnabled(anyString())).thenReturn(true);
        lenient().when(socialProviderSettingsService.isEnabled()).thenReturn(true);
    }

    @Mock private UserRepository userRepository;
    @Mock private SocialIdentityRepository socialIdentityRepository;
    @Mock private AuthorityRepository authorityRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginSettingsService loginSettingsService;
    @Mock private SocialProviderSettingsService socialProviderSettingsService;
    @Mock private SocialIdentityMapperService socialIdentityMapperService;
    @Mock private ObjectMapper objectMapper;
    @Mock private UserAccessInvalidationService userAccessInvalidationService;
    @Mock private AdminAuditEventService auditEventService;

    @Test
    void createsUserWithDefaultRoleAndStableSocialIdentity() {
        AuthorityEntity userRole = new AuthorityEntity(2L, AuthoritiesConstants.USER);
        when(socialIdentityRepository.findByProviderAndSubject("google", "google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ada@example.test")).thenReturn(Optional.empty());
        when(authorityRepository.findByName(AuthoritiesConstants.USER))
                .thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-password");
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(
                        invocation -> {
                            UserEntity user = invocation.getArgument(0);
                            user.setId(42L);
                            return user;
                        });

        String username = service().findOrCreate(authentication());

        assertThat(username).startsWith("social_google_");
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity created = userCaptor.getValue();
        assertThat(created.getEmail()).isEqualTo("ada@example.test");
        assertThat(created.isEmailVerified()).isTrue();
        assertThat(created.getPictureUrl()).isEqualTo("https://images.example.test/ada.jpg");
        assertThat(created.getAuthorities()).containsExactly(userRole);
        ArgumentCaptor<SocialIdentityEntity> identityCaptor =
                ArgumentCaptor.forClass(SocialIdentityEntity.class);
        verify(socialIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo("google");
        assertThat(identityCaptor.getValue().getSubject()).isEqualTo("google-subject");
        assertThat(identityCaptor.getValue().getUser()).isSameAs(created);
    }

    @Test
    void doesNotTrustAnUnverifiedSocialEmail() {
        AuthorityEntity userRole = new AuthorityEntity(2L, AuthoritiesConstants.USER);
        when(socialIdentityRepository.findByProviderAndSubject("google", "google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ada@example.test")).thenReturn(Optional.empty());
        when(authorityRepository.findByName(AuthoritiesConstants.USER))
                .thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-password");
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(
                        invocation -> {
                            UserEntity user = invocation.getArgument(0);
                            user.setId(42L);
                            return user;
                        });

        OAuth2User user =
                new DefaultOAuth2User(
                        java.util.Set.of(new SimpleGrantedAuthority("ROLE_OAUTH2_USER")),
                        Map.of(
                                "sub", "google-subject",
                                "email", "ada@example.test"),
                        "sub");
        OAuth2AuthenticationToken unverified =
                new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");

        service().findOrCreate(unverified);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isEmailVerified()).isFalse();
    }

    @Test
    void trustsEmailWhenProviderPolicyAllowsIt() {
        AuthorityEntity userRole = new AuthorityEntity(2L, AuthoritiesConstants.USER);
        when(socialProviderSettingsService.provider("google"))
                .thenReturn(
                        new SocialProviderSettingsService.ProviderCredentials(
                                "google",
                                "google",
                                "google-id",
                                "google-secret",
                                true,
                                false,
                                false,
                                true,
                                false,
                                "sub,email",
                                false,
                                false,
                                10,
                                "always"));
        when(socialIdentityRepository.findByProviderAndSubject("google", "google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ada@example.test")).thenReturn(Optional.empty());
        when(authorityRepository.findByName(AuthoritiesConstants.USER))
                .thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-password");
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(
                        invocation -> {
                            UserEntity user = invocation.getArgument(0);
                            user.setId(42L);
                            return user;
                        });

        OAuth2User user =
                new DefaultOAuth2User(
                        java.util.Set.of(new SimpleGrantedAuthority("ROLE_OAUTH2_USER")),
                        Map.of("sub", "google-subject", "email", "ada@example.test"),
                        "sub");
        service()
                .findOrCreate(new OAuth2AuthenticationToken(user, user.getAuthorities(), "google"));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isEmailVerified()).isTrue();
    }

    @Test
    void rejectsAuthenticationWhenAnEssentialClaimIsMissing() {
        when(socialProviderSettingsService.provider("google"))
                .thenReturn(
                        new SocialProviderSettingsService.ProviderCredentials(
                                "google",
                                "google",
                                "google-id",
                                "google-secret",
                                true,
                                false,
                                false,
                                false,
                                false,
                                "sub,email",
                                false,
                                false,
                                10,
                                "always"));
        OAuth2User user =
                new DefaultOAuth2User(
                        java.util.Set.of(new SimpleGrantedAuthority("ROLE_OAUTH2_USER")),
                        Map.of("sub", "google-subject"),
                        "sub");

        assertThatThrownBy(
                        () ->
                                service()
                                        .findOrCreate(
                                                new OAuth2AuthenticationToken(
                                                        user, user.getAuthorities(), "google")))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("required claim");
    }

    @Test
    void reusesUserForPreviouslyLinkedSocialIdentity() {
        UserEntity user = new UserEntity();
        user.setUsername("social_google_existing");
        when(socialIdentityRepository.findByProviderAndSubject("google", "google-subject"))
                .thenReturn(
                        Optional.of(new SocialIdentityEntity("google", "google-subject", user)));

        assertThat(service().findOrCreate(authentication())).isEqualTo("social_google_existing");
        verify(userRepository).save(user);
        assertThat(user.getPictureUrl()).isEqualTo("https://images.example.test/ada.jpg");
        verify(socialIdentityRepository, never()).save(any(SocialIdentityEntity.class));
    }

    @Test
    void forceSyncModeUpdatesExistingUserProfile() {
        UserEntity user = new UserEntity();
        user.setUsername("social_google_existing");
        user.setFirstName("Old");
        user.setLastName("Name");
        user.setEmail("old@example.test");
        when(socialProviderSettingsService.syncMode("google")).thenReturn("force");
        when(socialIdentityRepository.findByProviderAndSubject("google", "google-subject"))
                .thenReturn(
                        Optional.of(new SocialIdentityEntity("google", "google-subject", user)));

        assertThat(service().findOrCreate(authentication())).isEqualTo("social_google_existing");
        assertThat(user.getFirstName()).isEqualTo("Ada");
        assertThat(user.getLastName()).isEqualTo("Lovelace");
        assertThat(user.getEmail()).isEqualTo("ada@example.test");
        assertThat(user.getPictureUrl()).isEqualTo("https://images.example.test/ada.jpg");
        verify(userRepository).save(user);
    }

    @Test
    void doesNotSilentlyLinkSocialIdentityToExistingEmailAccount() {
        UserEntity existing = new UserEntity();
        existing.setUsername("ada");
        when(socialIdentityRepository.findByProviderAndSubject("google", "google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ada@example.test"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service().findOrCreate(authentication()))
                .isInstanceOf(
                        org.springframework.security.oauth2.core.OAuth2AuthenticationException
                                .class)
                .hasMessageContaining("already exists");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void linksPendingSocialIdentityToExistingLocalAccount() {
        UserEntity existing = new UserEntity();
        existing.setId(42L);
        existing.setUsername("ada");
        when(userRepository.findForLoginUpdate("ada")).thenReturn(Optional.of(existing));
        when(socialIdentityRepository.findByProviderAndSubject("github", "123"))
                .thenReturn(Optional.empty());

        service().linkPending("ada", Map.of("provider", "github", "subject", "123"));

        ArgumentCaptor<SocialIdentityEntity> identityCaptor =
                ArgumentCaptor.forClass(SocialIdentityEntity.class);
        verify(socialIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo("github");
        assertThat(identityCaptor.getValue().getSubject()).isEqualTo("123");
        assertThat(identityCaptor.getValue().getUser()).isSameAs(existing);
    }

    @Test
    void linkingAlreadyLinkedIdentityIsIdempotentForSameLocalAccount() {
        UserEntity existing = new UserEntity();
        existing.setId(42L);
        existing.setUsername("ada");
        SocialIdentityEntity identity = new SocialIdentityEntity("github", "123", existing);
        when(userRepository.findForLoginUpdate("ada")).thenReturn(Optional.of(existing));
        when(socialIdentityRepository.findByProviderAndSubject("github", "123"))
                .thenReturn(Optional.of(identity));

        service().linkPending("ada", Map.of("provider", "github", "subject", "123"));

        verify(socialIdentityRepository, never()).save(any(SocialIdentityEntity.class));
    }

    @Test
    void rejectsLinkingAnotherIdentityFromSameProviderWithoutOverrideFlow() {
        UserEntity existing = new UserEntity();
        existing.setId(42L);
        existing.setUsername("ada");
        when(userRepository.findForLoginUpdate("ada")).thenReturn(Optional.of(existing));
        when(socialIdentityRepository.findByProviderAndSubject("github", "456"))
                .thenReturn(Optional.empty());
        when(socialIdentityRepository.findAllByUserUsernameAndProvider("ada", "github"))
                .thenReturn(java.util.List.of(new SocialIdentityEntity("github", "123", existing)));

        assertThatThrownBy(
                        () ->
                                service()
                                        .linkPending(
                                                "ada",
                                                Map.of("provider", "github", "subject", "456")))
                .isInstanceOf(
                        org.springframework.security.oauth2.core.OAuth2AuthenticationException
                                .class)
                .hasMessageContaining("already linked");
        verify(socialIdentityRepository, never()).save(any(SocialIdentityEntity.class));
    }

    @Test
    void listsAllConfiguredProviders() {
        SocialLoginProperties properties = new SocialLoginProperties();
        properties.google().setClientId("google-id");
        properties.google().setClientSecret("google-secret");
        properties.github().setClientId("github-id");
        properties.github().setClientSecret("github-secret");
        properties.linkedin().setClientId("linkedin-id");
        properties.linkedin().setClientSecret("linkedin-secret");
        properties.microsoft().setClientId("microsoft-id");
        properties.microsoft().setClientSecret("microsoft-secret");

        assertThat(service().configuredProviders(properties))
                .containsExactly("github", "google", "linkedin", "microsoft");
    }

    @Test
    void excludesDisabledConfiguredProvider() {
        SocialLoginProperties properties = new SocialLoginProperties();
        properties.google().setClientId("google-id");
        properties.google().setClientSecret("google-secret");
        properties.github().setClientId("github-id");
        properties.github().setClientSecret("github-secret");
        properties.linkedin().setClientId("linkedin-id");
        properties.linkedin().setClientSecret("linkedin-secret");
        when(loginSettingsService.isSocialProviderEnabled("github")).thenReturn(false);

        assertThat(service().configuredProviders(properties)).containsExactly("google", "linkedin");
    }

    @Test
    void excludesDisabledProviderFromStoredConfiguration() {
        when(socialProviderSettingsService.configuredProviders())
                .thenReturn(
                        java.util.List.of(
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "google", "google-id", "google-secret"),
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "github", "github-id", "github-secret"),
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "linkedin", "linkedin-id", "linkedin-secret"),
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "microsoft", "microsoft-id", "microsoft-secret")));
        when(loginSettingsService.isSocialProviderEnabled("github")).thenReturn(false);

        assertThat(service().configuredProviders())
                .containsExactly("google", "linkedin", "microsoft");
    }

    @Test
    void listsEnabledProvidersIncludingUnconfiguredProviders() {
        when(socialProviderSettingsService.effectiveProviders())
                .thenReturn(
                        java.util.List.of(
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "google", "google-id", "google-secret"),
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "microsoft", "", "")));
        when(loginSettingsService.isSocialProviderEnabled(anyString())).thenReturn(true);

        assertThat(service().availableProviders())
                .extracting(provider -> provider.provider() + ":" + provider.configured())
                .containsExactly("google:true", "microsoft:false");
    }

    @Test
    void hidesProvidersWhenSocialLoginIsGloballyDisabled() {
        when(socialProviderSettingsService.isEnabled()).thenReturn(false);
        when(socialProviderSettingsService.effectiveProviders())
                .thenReturn(
                        java.util.List.of(
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "google", "google-id", "google-secret")));

        assertThat(service().availableProviders()).isEmpty();
    }

    @Test
    void listsEnabledSocialLinksIncludingUnconfiguredMicrosoft() {
        when(socialProviderSettingsService.effectiveProviders())
                .thenReturn(
                        java.util.List.of(
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "google", "google-id", "google-secret"),
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "microsoft", "", "")));
        when(socialIdentityRepository.findAllByUserUsername("ada")).thenReturn(java.util.List.of());

        assertThat(service().socialLinks("ada"))
                .extracting(link -> link.provider() + ":" + link.linked() + ":" + link.configured())
                .containsExactly("google:false:true", "microsoft:false:false");
    }

    @Test
    void keepsLinkedProviderVisibleWhenAdministratorDisablesIt() {
        when(socialProviderSettingsService.effectiveProviders())
                .thenReturn(
                        java.util.List.of(
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "github", "github-id", "github-secret")));
        UserEntity user = new UserEntity();
        user.setUsername("ada");
        when(socialIdentityRepository.findAllByUserUsername("ada"))
                .thenReturn(java.util.List.of(new SocialIdentityEntity("github", "123", user)));
        when(loginSettingsService.isSocialProviderEnabled("github")).thenReturn(false);

        assertThat(service().socialLinks("ada"))
                .extracting(
                        link ->
                                link.provider()
                                        + ":"
                                        + link.linked()
                                        + ":"
                                        + link.configured()
                                        + ":"
                                        + link.enabled())
                .containsExactly("github:true:true:false");
    }

    @Test
    void rejectsAuthenticationWhenProviderWasDisabledAfterLoginPageLoad() {
        when(loginSettingsService.isSocialProviderEnabled("google")).thenReturn(false);

        assertThatThrownBy(() -> service().findOrCreate(authentication()))
                .isInstanceOf(
                        org.springframework.security.oauth2.core.OAuth2AuthenticationException
                                .class)
                .hasMessageContaining("disabled");
        verify(socialIdentityRepository, never())
                .findByProviderAndSubject(anyString(), anyString());
    }

    @Test
    void rejectsPublicLoginForAccountLinkingOnlyProvider() {
        when(socialProviderSettingsService.provider("google"))
                .thenReturn(
                        new SocialProviderSettingsService.ProviderCredentials(
                                "google",
                                "google",
                                "google-id",
                                "google-secret",
                                true,
                                false,
                                true,
                                10,
                                "always"));

        assertThatThrownBy(() -> service().findOrCreate(authentication()))
                .isInstanceOf(
                        org.springframework.security.oauth2.core.OAuth2AuthenticationException
                                .class)
                .hasMessageContaining("disabled for public sign-in");
        verify(socialIdentityRepository, never())
                .findByProviderAndSubject(anyString(), anyString());
    }

    @Test
    void usesAliasesAndGuiOrderForPublicProviders() {
        when(socialProviderSettingsService.effectiveProviders())
                .thenReturn(
                        java.util.List.of(
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "google",
                                        "z-login",
                                        "google-id",
                                        "google-secret",
                                        true,
                                        false,
                                        false,
                                        20,
                                        "always"),
                                new SocialProviderSettingsService.ProviderCredentials(
                                        "github",
                                        "a-login",
                                        "github-id",
                                        "github-secret",
                                        true,
                                        false,
                                        false,
                                        10,
                                        "always")));

        assertThat(service().availableProviders())
                .extracting(provider -> provider.provider() + ":" + provider.configured())
                .containsExactly("a-login:true", "z-login:true");
    }

    @Test
    void unlinksAllIdentitiesForProviderAndInvalidatesAccess() {
        UserEntity user = new UserEntity();
        user.setUsername("ada");
        SocialIdentityEntity identity = new SocialIdentityEntity("github", "123", user);
        when(socialIdentityRepository.findAllByUserUsernameAndProvider("ada", "github"))
                .thenReturn(java.util.List.of(identity));

        service().unlink("ada", "github");

        verify(socialIdentityRepository).deleteAll(java.util.List.of(identity));
        verify(userAccessInvalidationService).invalidate("ada");
        verify(auditEventService)
                .record("account.social_link.deleted", "social_identity", "ada:github");
    }

    private SocialLoginService service() {
        return new SocialLoginService(
                userRepository,
                socialIdentityRepository,
                authorityRepository,
                passwordEncoder,
                loginSettingsService,
                socialProviderSettingsService,
                socialIdentityMapperService,
                objectMapper,
                userAccessInvalidationService,
                auditEventService);
    }

    private static OAuth2AuthenticationToken authentication() {
        OAuth2User user =
                new DefaultOAuth2User(
                        java.util.Set.of(new SimpleGrantedAuthority("ROLE_OAUTH2_USER")),
                        Map.of(
                                "sub", "google-subject",
                                "email", "ada@example.test",
                                "email_verified", true,
                                "given_name", "Ada",
                                "family_name", "Lovelace",
                                "picture", "https://images.example.test/ada.jpg"),
                        "sub");
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
    }
}
