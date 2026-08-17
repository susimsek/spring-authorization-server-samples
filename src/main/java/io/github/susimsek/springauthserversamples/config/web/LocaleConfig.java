package io.github.susimsek.springauthserversamples.config.web;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

@Configuration(proxyBeanMethods = false)
public class LocaleConfig {

    public static final String LOCALE_COOKIE_NAME = "AUTH_LOCALE";
    public static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "tr");

    @Bean
    LocaleResolver localeResolver(WebProperties webProperties) {
        CookieLocaleResolver resolver = new CookieLocaleResolver(LOCALE_COOKIE_NAME);
        resolver.setCookiePath("/");
        resolver.setCookieMaxAge(Duration.ofDays(365));
        resolver.setCookieSameSite("Lax");
        resolver.setDefaultLocaleFunction(
                request -> {
                    Locale requested = request.getLocale();
                    if (isSupported(requested)) {
                        return normalize(requested);
                    }

                    Locale configured = webProperties.getLocale();
                    return isSupported(configured) ? normalize(configured) : Locale.ENGLISH;
                });
        return resolver;
    }

    public static boolean isSupported(Locale locale) {
        return locale != null
                && SUPPORTED_LANGUAGES.contains(locale.getLanguage().toLowerCase(Locale.ROOT));
    }

    public static Locale normalize(Locale locale) {
        return Locale.forLanguageTag(locale.getLanguage().toLowerCase(Locale.ROOT));
    }

    private LocaleConfig() {}
}
