package io.github.susimsek.springauthserversamples.config.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.admin.LocalizationSettingsService;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

class LocaleConfigTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supportsAndNormalizesConfiguredLanguages() {
        assertThat(LocaleConfig.isSupported(Locale.forLanguageTag("TR"))).isTrue();
        assertThat(LocaleConfig.isSupported(Locale.GERMAN)).isFalse();
        assertThat(LocaleConfig.normalize(Locale.forLanguageTag("TR"))).isEqualTo(Locale.of("tr"));
    }

    @Test
    void resolverUsesRequestedLocaleThenConfiguredFallbackThenEnglish() {
        LocaleConfig config = localeConfig();
        WebProperties webProperties = new WebProperties();
        webProperties.setLocale(Locale.of("tr"));
        CookieLocaleResolver resolver = (CookieLocaleResolver) config.localeResolver(webProperties);

        MockHttpServletRequest supportedRequest = new MockHttpServletRequest();
        supportedRequest.addPreferredLocale(Locale.of("tr"));
        assertThat(resolver.resolveLocale(supportedRequest)).isEqualTo(Locale.of("tr"));

        MockHttpServletRequest unsupportedRequest = new MockHttpServletRequest();
        unsupportedRequest.addPreferredLocale(Locale.GERMAN);
        assertThat(resolver.resolveLocale(unsupportedRequest)).isEqualTo(Locale.of("tr"));
    }

    @Test
    void selectedCookieLocaleOverridesAuthenticatedUsersPreferredLocale() {
        LocalizationSettingsService settings = mock(LocalizationSettingsService.class);
        UserRepository users = mock(UserRepository.class);
        UserEntity user = new UserEntity();
        user.setPreferredLocale("tr");
        when(settings.isInternationalizationEnabled()).thenReturn(true);
        when(settings.isSupported(Locale.of("en"))).thenReturn(true);
        when(settings.isSupported(Locale.of("tr"))).thenReturn(true);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new TestingAuthenticationToken("alice", "token", java.util.List.of()));

        CookieLocaleResolver resolver =
                (CookieLocaleResolver)
                        new LocaleConfig(settings, users).localeResolver(new WebProperties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("locale", "en"));
        request.addPreferredLocale(Locale.of("en"));

        assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.of("en"));
    }

    @Test
    void authenticatedUsersPreferredLocaleOverridesExplicitUiLocales() {
        LocalizationSettingsService settings = mock(LocalizationSettingsService.class);
        UserRepository users = mock(UserRepository.class);
        UserEntity user = new UserEntity();
        user.setPreferredLocale("tr");
        when(settings.isInternationalizationEnabled()).thenReturn(true);
        when(settings.isSupported(Locale.of("en"))).thenReturn(true);
        when(settings.isSupported(Locale.of("tr"))).thenReturn(true);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new TestingAuthenticationToken("alice", "token", java.util.List.of()));

        CookieLocaleResolver resolver =
                (CookieLocaleResolver)
                        new LocaleConfig(settings, users).localeResolver(new WebProperties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("ui_locales", "en tr");

        assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.of("tr"));
    }

    private static LocaleConfig localeConfig() {
        try {
            var constructor = LocaleConfig.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
