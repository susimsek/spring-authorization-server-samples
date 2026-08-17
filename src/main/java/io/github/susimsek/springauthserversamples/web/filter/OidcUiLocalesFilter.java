package io.github.susimsek.springauthserversamples.web.filter;

import io.github.susimsek.springauthserversamples.config.web.LocaleConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class OidcUiLocalesFilter extends OncePerRequestFilter {

    private static final String UI_LOCALES_PARAMETER = "ui_locales";

    private final LocaleResolver localeResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uiLocales = request.getParameter(UI_LOCALES_PARAMETER);
        if (StringUtils.hasText(uiLocales)) {
            Arrays.stream(uiLocales.split("\\s+"))
                    .map(Locale::forLanguageTag)
                    .filter(LocaleConfig::isSupported)
                    .findFirst()
                    .map(LocaleConfig::normalize)
                    .ifPresent(locale -> localeResolver.setLocale(request, response, locale));
        }

        filterChain.doFilter(request, response);
    }
}
