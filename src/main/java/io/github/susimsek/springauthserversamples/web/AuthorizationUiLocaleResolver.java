package io.github.susimsek.springauthserversamples.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthorizationUiLocaleResolver {

    static final String LOCALE_COOKIE_NAME = "AUTH_LOCALE";

    private static final String UI_LOCALES_PARAMETER = "ui_locales";
    private static final String DEFAULT_LOCALE = "en";
    private static final Set<String> SUPPORTED_LOCALES = Set.of("en", "tr");

    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    public String resolve(HttpServletRequest request, HttpServletResponse response) {
        return resolveCookie(request)
                .or(() -> resolveUiLocales(request, response))
                .or(() -> resolveAcceptLanguage(request))
                .orElse(DEFAULT_LOCALE);
    }

    boolean isSupportedLocale(String languageTag) {
        return resolveSupportedLocale(languageTag).isPresent();
    }

    Set<String> supportedLocales() {
        return SUPPORTED_LOCALES;
    }

    private Optional<String> resolveCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> LOCALE_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .map(this::resolveSupportedLocale)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<String> resolveUiLocales(
            HttpServletRequest request, HttpServletResponse response) {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null) {
            return Optional.empty();
        }

        String query = URI.create(savedRequest.getRedirectUrl()).getRawQuery();
        if (!StringUtils.hasText(query)) {
            return Optional.empty();
        }

        return Stream.of(query.split("&"))
                .map(parameter -> parameter.split("=", 2))
                .filter(parts -> parts.length == 2 && UI_LOCALES_PARAMETER.equals(parts[0]))
                .map(parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8))
                .flatMap(value -> Stream.of(value.split("\\s+")))
                .map(this::resolveSupportedLocale)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<String> resolveAcceptLanguage(HttpServletRequest request) {
        return resolveSupportedLocale(request.getLocale().toLanguageTag());
    }

    private Optional<String> resolveSupportedLocale(String languageTag) {
        if (!StringUtils.hasText(languageTag)) {
            return Optional.empty();
        }

        String language = Locale.forLanguageTag(languageTag).getLanguage().toLowerCase(Locale.ROOT);
        return SUPPORTED_LOCALES.contains(language) ? Optional.of(language) : Optional.empty();
    }
}
