package io.github.susimsek.springauthserversamples.web.filter;

import io.github.susimsek.springauthserversamples.config.web.LocaleConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class SpaFilter extends OncePerRequestFilter {

    private static final String STATIC_CLASSPATH_PREFIX = "classpath:/static";

    private final LocaleResolver localeResolver;
    private final ResourceLoader resourceLoader;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = requestPath(request);
        if (!isSpaRouteCandidate(request, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String firstSegment = firstSegment(path);
        if (isSupportedLanguage(firstSegment)) {
            forwardIfExists(path, request, response, filterChain);
            return;
        }

        if (!hasLocalizedPage(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String locale = localeResolver.resolveLocale(request).getLanguage();
        String localizedPath = "/".equals(path) ? "/" + locale : "/" + locale + path;
        forwardIfExists(localizedPath, request, response, filterChain);
    }

    private boolean hasLocalizedPage(String path) {
        return LocaleConfig.SUPPORTED_LANGUAGES.stream()
                .map(locale -> "/".equals(path) ? "/" + locale : "/" + locale + path)
                .map(SpaFilter::toIndexPath)
                .anyMatch(this::resourceExists);
    }

    private void forwardIfExists(
            String localizedPath,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String indexPath = toIndexPath(localizedPath);
        if (!resourceExists(indexPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher(indexPath);
        dispatcher.forward(request, response);
    }

    private boolean resourceExists(String indexPath) {
        return resourceLoader.getResource(STATIC_CLASSPATH_PREFIX + indexPath).exists();
    }

    private static boolean isSpaRouteCandidate(HttpServletRequest request, String path) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        if (!StringUtils.hasText(path) || path.contains("..") || path.indexOf('\\') >= 0) {
            return false;
        }

        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        return !lastSegment.contains(".");
    }

    private static String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        String path =
                StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)
                        ? requestUri.substring(contextPath.length())
                        : requestUri;

        if (!StringUtils.hasText(path)) {
            return "/";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static String firstSegment(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            return "";
        }

        int nextSlash = path.indexOf('/', 1);
        return nextSlash < 0 ? path.substring(1) : path.substring(1, nextSlash);
    }

    private static boolean isSupportedLanguage(String language) {
        return StringUtils.hasText(language)
                && LocaleConfig.SUPPORTED_LANGUAGES.contains(language.toLowerCase(Locale.ROOT));
    }

    private static String toIndexPath(String localizedPath) {
        return "/".equals(localizedPath) ? "/index.html" : localizedPath + "/index.html";
    }
}
