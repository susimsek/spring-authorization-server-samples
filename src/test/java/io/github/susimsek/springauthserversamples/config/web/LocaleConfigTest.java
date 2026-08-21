package io.github.susimsek.springauthserversamples.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

class LocaleConfigTest {

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
