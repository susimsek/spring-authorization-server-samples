package io.github.susimsek.springauthserversamples.web.filter;

import io.github.susimsek.springauthserversamples.config.web.LocaleConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    /**
     * Static-export templates for entity detail routes. An empty section set means the entity has a
     * detail page directly beneath its identifier.
     */
    private static final Map<String, Set<String>> DYNAMIC_ADMIN_ROUTE_SECTIONS =
            Map.of(
                    "roles", Set.of(),
                    "consents", Set.of(),
                    "groups", Set.of(),
                    "clients",
                            Set.of(
                                    "settings",
                                    "credentials",
                                    "scopes",
                                    "sessions",
                                    "consents",
                                    "events"),
                    "users",
                            Set.of(
                                    "details",
                                    "credentials",
                                    "roles",
                                    "sessions",
                                    "consents",
                                    "events"));

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

    private static List<String> toDynamicEntityResourcePaths(String localizedPath) {
        String[] segments = localizedPath.split("/");
        if (segments.length < 5 || !"admin".equals(segments[2])) {
            return List.of();
        }

        String resource = segments[3];
        boolean dataRequest = localizedPath.endsWith(".txt");
        String entityId = dataRequest ? removeTxtExtension(segments[4]) : segments[4];
        boolean entityIndexDataRequest =
                dataRequest && segments.length == 6 && "index.txt".equals(segments[5]);
        if (!StringUtils.hasText(entityId) || "_".equals(entityId)) {
            return List.of();
        }

        Set<String> sections = DYNAMIC_ADMIN_ROUTE_SECTIONS.get(resource);
        if (sections == null) {
            return List.of();
        }

        if (sections.isEmpty()) {
            return segments.length == 5 || entityIndexDataRequest
                    ? List.of(dynamicIndexPath(segments[1], resource, dataRequest))
                    : List.of();
        }

        String section = segments.length < 6 ? "" : removeTxtExtension(segments[5]);
        if (!sections.contains(section)) {
            return List.of();
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
            return List.of(template + (dataRequest ? "/index.txt" : "/index.html"));
        }

        for (int i = 6; i < segments.length; i++) {
            template.append('/').append(segments[i]);
        }
        return List.of(template.toString());
    }

    private static String dynamicIndexPath(String locale, String resource, boolean dataRequest) {
        return "/" + locale + "/admin/" + resource + "/_/index" + (dataRequest ? ".txt" : ".html");
    }

    private static String removeTxtExtension(String value) {
        return value.endsWith(".txt") ? value.substring(0, value.length() - 4) : value;
    }

    private boolean resourceExists(String indexPath) {
        return resourceLoader.getResource(STATIC_CLASSPATH_PREFIX + indexPath).exists();
    }

    private static boolean isSpaRouteCandidate(HttpServletRequest request, String path) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
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
