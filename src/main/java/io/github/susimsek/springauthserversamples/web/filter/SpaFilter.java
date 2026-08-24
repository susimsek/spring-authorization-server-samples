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

        redirectToLocalizedPath(localizedPath, request, response, filterChain);
    }

    private boolean hasLocalizedPage(String path) {
        return LocaleConfig.SUPPORTED_LANGUAGES.stream()
                .map(locale -> "/".equals(path) ? "/" + locale : "/" + locale + path)
                .map(this::resolveIndexPath)
                .anyMatch(StringUtils::hasText);
    }

    private void forwardIfExists(
            String localizedPath,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String indexPath = resolveIndexPath(localizedPath);
        if (!StringUtils.hasText(indexPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher(indexPath);
        dispatcher.forward(request, response);
    }

    private void redirectToLocalizedPath(
            String localizedPath,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws IOException, ServletException {
        if (!StringUtils.hasText(resolveIndexPath(localizedPath))) {
            filterChain.doFilter(request, response);
            return;
        }

        String query = request.getQueryString();
        response.sendRedirect(
                StringUtils.hasText(query) ? localizedPath + "?" + query : localizedPath);
    }

    private String resolveIndexPath(String localizedPath) {
        String exactResourcePath = toExactResourcePath(localizedPath);
        if (resourceExists(exactResourcePath)) {
            return exactResourcePath;
        }

        for (String dynamicResourcePath : toDynamicEntityResourcePaths(localizedPath)) {
            if (resourceExists(dynamicResourcePath)) {
                return dynamicResourcePath;
            }
        }
        return null;
    }

    private static java.util.List<String> toDynamicEntityResourcePaths(String localizedPath) {
        String[] segments = localizedPath.split("/");
        if (segments.length < 5 || !"admin".equals(segments[2])) {
            return java.util.List.of();
        }

        String resource = segments[3];
        String entityId = segments[4];
        if (!StringUtils.hasText(entityId) || "_".equals(entityId)) {
            return java.util.List.of();
        }

        if ("roles".equals(resource) && segments.length == 5) {
            return java.util.List.of("/" + segments[1] + "/admin/roles/_/index.html");
        }
        if ("consents".equals(resource) && segments.length == 5) {
            return java.util.List.of("/" + segments[1] + "/admin/consents/_/index.html");
        }

        if (segments.length < 6) {
            return java.util.List.of();
        }

        String section = segments[5];

        boolean supportedSection =
                switch (resource) {
                    case "clients" ->
                            java.util.Set.of(
                                            "settings",
                                            "credentials",
                                            "scopes",
                                            "sessions",
                                            "consents",
                                            "events")
                                    .contains(section);
                    case "users" ->
                            java.util.Set.of(
                                            "details",
                                            "credentials",
                                            "roles",
                                            "sessions",
                                            "consents",
                                            "events")
                                    .contains(section);
                    default -> false;
                };

        if (!supportedSection) {
            return java.util.List.of();
        }

        StringBuilder template =
                new StringBuilder()
                        .append('/')
                        .append(segments[1])
                        .append("/admin/")
                        .append(resource)
                        .append("/_/")
                        .append(section);

        if (segments.length == 6) {
            return java.util.List.of(template + "/index.html");
        }

        for (int i = 6; i < segments.length; i++) {
            template.append('/').append(segments[i]);
        }
        return java.util.List.of(template.toString());
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
        return !lastSegment.contains(".") || path.endsWith(".txt");
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

    private static String toExactResourcePath(String localizedPath) {
        if (localizedPath.endsWith(".txt")) {
            return localizedPath;
        }
        return "/".equals(localizedPath) ? "/index.html" : localizedPath + "/index.html";
    }
}
