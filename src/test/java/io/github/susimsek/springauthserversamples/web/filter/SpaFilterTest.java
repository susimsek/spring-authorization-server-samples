package io.github.susimsek.springauthserversamples.web.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.LocaleResolver;

class SpaFilterTest {

    private final LocaleResolver localeResolver = mock(LocaleResolver.class);
    private final ResourceLoader resourceLoader = mock(ResourceLoader.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain filterChain = mock(FilterChain.class);
    private final RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    private SpaFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SpaFilter(localeResolver, resourceLoader);
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("");
    }

    @Test
    void redirectsLocaleLessRouteToResolvedLocalizedPage() throws Exception {
        Resource english = mock(Resource.class);
        Resource turkish = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/login");
        when(resourceLoader.getResource("classpath:/static/en/login/index.html"))
                .thenReturn(english);
        when(resourceLoader.getResource("classpath:/static/tr/login/index.html"))
                .thenReturn(turkish);
        when(english.exists()).thenReturn(true);

        when(localeResolver.resolveLocale(request)).thenReturn(Locale.forLanguageTag("tr"));
        when(turkish.exists()).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendRedirect("/tr/login");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void redirectsLocaleLessAdminRouteToLocalizedAdminPage() throws Exception {
        Resource english = mock(Resource.class);
        Resource turkish = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/admin");
        when(resourceLoader.getResource("classpath:/static/en/admin/index.html"))
                .thenReturn(english);
        when(resourceLoader.getResource("classpath:/static/tr/admin/index.html"))
                .thenReturn(turkish);
        when(english.exists()).thenReturn(true);
        when(turkish.exists()).thenReturn(true);
        when(localeResolver.resolveLocale(request)).thenReturn(Locale.forLanguageTag("tr"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendRedirect("/tr/admin");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void preservesQueryWhenRedirectingLocaleLessRoute() throws Exception {
        Resource english = mock(Resource.class);
        Resource turkish = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/error");
        when(request.getQueryString()).thenReturn("type=not_found");
        when(resourceLoader.getResource("classpath:/static/en/error/index.html"))
                .thenReturn(english);
        when(resourceLoader.getResource("classpath:/static/tr/error/index.html"))
                .thenReturn(turkish);
        when(english.exists()).thenReturn(true);
        when(localeResolver.resolveLocale(request)).thenReturn(Locale.forLanguageTag("tr"));
        when(turkish.exists()).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendRedirect("/tr/error?type=not_found");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void forwardsAlreadyLocalizedRouteDirectly() throws Exception {
        Resource resource = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/en/login/");
        when(resourceLoader.getResource("classpath:/static/en/login/index.html"))
                .thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/en/login/index.html")).thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(localeResolver, never()).resolveLocale(request);
    }

    @Test
    void forwardsHeadRequestUsedBySpaLinkPrefetch() throws Exception {
        Resource resource = mock(Resource.class);

        when(request.getMethod()).thenReturn("HEAD");
        when(request.getRequestURI()).thenReturn("/en/admin/sessions/");
        when(resourceLoader.getResource("classpath:/static/en/admin/sessions/index.html"))
                .thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/en/admin/sessions/index.html")).thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void continuesChainWhenNoLocalizedStaticPageExists() throws Exception {
        Resource english = mock(Resource.class);
        Resource turkish = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/oauth2/authorize");
        when(resourceLoader.getResource("classpath:/static/en/oauth2/authorize/index.html"))
                .thenReturn(english);
        when(resourceLoader.getResource("classpath:/static/tr/oauth2/authorize/index.html"))
                .thenReturn(turkish);
        when(english.exists()).thenReturn(false);
        when(turkish.exists()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(localeResolver, never()).resolveLocale(request);
    }

    @Test
    void ignoresStaticAssets() throws Exception {
        when(request.getRequestURI()).thenReturn("/_next/static/app.js");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void ignoresNonGetRequests() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void ignoresUnsafeOrMalformedPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/../admin");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void continuesChainForMissingLocalizedRoute() throws Exception {
        Resource resource = mock(Resource.class);
        when(request.getRequestURI()).thenReturn("/tr/login");
        when(resourceLoader.getResource("classpath:/static/tr/login/index.html"))
                .thenReturn(resource);
        when(resource.exists()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void continuesChainWhenResolvedLocalePageDisappearsBeforeRedirect() throws Exception {
        Resource english = mock(Resource.class);
        Resource turkish = mock(Resource.class);
        when(request.getRequestURI()).thenReturn("/login");
        when(resourceLoader.getResource("classpath:/static/en/login/index.html"))
                .thenReturn(english);
        when(resourceLoader.getResource("classpath:/static/tr/login/index.html"))
                .thenReturn(turkish);
        when(english.exists()).thenReturn(true);
        when(localeResolver.resolveLocale(request)).thenReturn(Locale.forLanguageTag("tr"));
        when(turkish.exists()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void removesContextPathAndNormalizesTrailingSlash() throws Exception {
        Resource resource = mock(Resource.class);
        when(request.getContextPath()).thenReturn("/server");
        when(request.getRequestURI()).thenReturn("/server/en/login/");
        when(resourceLoader.getResource("classpath:/static/en/login/index.html"))
                .thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/en/login/index.html")).thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
    }

    @Test
    void forwardsLocalizedDynamicClientRouteToExportedTemplate() throws Exception {
        Resource exact = mock(Resource.class);
        Resource template = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/en/admin/clients/client-123/settings");
        when(resourceLoader.getResource(
                        "classpath:/static/en/admin/clients/client-123/settings/index.html"))
                .thenReturn(exact);
        when(resourceLoader.getResource("classpath:/static/en/admin/clients/_/settings/index.html"))
                .thenReturn(template);
        when(exact.exists()).thenReturn(false);
        when(template.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/en/admin/clients/_/settings/index.html"))
                .thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void forwardsLocalizedDynamicGroupRouteToExportedTemplate() throws Exception {
        Resource exact = mock(Resource.class);
        Resource template = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/en/admin/groups/42");
        when(resourceLoader.getResource("classpath:/static/en/admin/groups/42/index.html"))
                .thenReturn(exact);
        when(resourceLoader.getResource("classpath:/static/en/admin/groups/_/index.html"))
                .thenReturn(template);
        when(exact.exists()).thenReturn(false);
        when(template.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/en/admin/groups/_/index.html")).thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void forwardsDynamicGroupDataRequestToExportedDataTemplate() throws Exception {
        Resource exact = mock(Resource.class);
        Resource template = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/en/admin/groups/42.txt");
        when(resourceLoader.getResource("classpath:/static/en/admin/groups/42.txt"))
                .thenReturn(exact);
        when(resourceLoader.getResource("classpath:/static/en/admin/groups/_/index.txt"))
                .thenReturn(template);
        when(exact.exists()).thenReturn(false);
        when(template.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/en/admin/groups/_/index.txt")).thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void forwardsDynamicGroupIndexDataRequestToExportedDataTemplate() throws Exception {
        Resource exact = mock(Resource.class);
        Resource template = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/en/admin/groups/42/index.txt");
        when(resourceLoader.getResource("classpath:/static/en/admin/groups/42/index.txt"))
                .thenReturn(exact);
        when(resourceLoader.getResource("classpath:/static/en/admin/groups/_/index.txt"))
                .thenReturn(template);
        when(exact.exists()).thenReturn(false);
        when(template.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/en/admin/groups/_/index.txt")).thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void redirectsLocaleLessDynamicUserRouteAndPreservesEntityPath() throws Exception {
        Resource englishExact = mock(Resource.class);
        Resource englishTemplate = mock(Resource.class);
        Resource turkishExact = mock(Resource.class);
        Resource turkishTemplate = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/admin/users/42/sessions");
        when(resourceLoader.getResource("classpath:/static/en/admin/users/42/sessions/index.html"))
                .thenReturn(englishExact);
        when(resourceLoader.getResource("classpath:/static/en/admin/users/_/sessions/index.html"))
                .thenReturn(englishTemplate);
        when(resourceLoader.getResource("classpath:/static/tr/admin/users/42/sessions/index.html"))
                .thenReturn(turkishExact);
        when(resourceLoader.getResource("classpath:/static/tr/admin/users/_/sessions/index.html"))
                .thenReturn(turkishTemplate);
        when(englishExact.exists()).thenReturn(false);
        when(englishTemplate.exists()).thenReturn(true);
        when(turkishExact.exists()).thenReturn(false);
        when(turkishTemplate.exists()).thenReturn(true);
        when(localeResolver.resolveLocale(request)).thenReturn(Locale.forLanguageTag("tr"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendRedirect("/tr/admin/users/42/sessions");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void rejectsUnsupportedDynamicAdminSection() throws Exception {
        Resource resource = mock(Resource.class);
        when(request.getRequestURI()).thenReturn("/en/admin/clients/client-123/unknown");
        when(resourceLoader.getResource(
                        "classpath:/static/en/admin/clients/client-123/unknown/index.html"))
                .thenReturn(resource);
        when(resource.exists()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void forwardsDynamicClientFlightPayloadToExportedTemplate() throws Exception {
        Resource exact = mock(Resource.class);
        Resource template = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/en/admin/clients/client-123/settings/index.txt");
        when(resourceLoader.getResource(
                        "classpath:/static/en/admin/clients/client-123/settings/index.txt"))
                .thenReturn(exact);
        when(resourceLoader.getResource("classpath:/static/en/admin/clients/_/settings/index.txt"))
                .thenReturn(template);
        when(exact.exists()).thenReturn(false);
        when(template.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/en/admin/clients/_/settings/index.txt"))
                .thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void forwardsDynamicUserFlightTreePayloadToExportedTemplate() throws Exception {
        Resource exact = mock(Resource.class);
        Resource template = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/tr/admin/users/42/sessions/__next._tree.txt");
        when(resourceLoader.getResource(
                        "classpath:/static/tr/admin/users/42/sessions/__next._tree.txt"))
                .thenReturn(exact);
        when(resourceLoader.getResource(
                        "classpath:/static/tr/admin/users/_/sessions/__next._tree.txt"))
                .thenReturn(template);
        when(exact.exists()).thenReturn(false);
        when(template.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/tr/admin/users/_/sessions/__next._tree.txt"))
                .thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void forwardsDynamicRoleDetailRouteToExportedTemplate() throws Exception {
        Resource exact = mock(Resource.class);
        Resource template = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/en/admin/roles/ROLE_AUDITOR");
        when(resourceLoader.getResource("classpath:/static/en/admin/roles/ROLE_AUDITOR/index.html"))
                .thenReturn(exact);
        when(resourceLoader.getResource("classpath:/static/en/admin/roles/_/index.html"))
                .thenReturn(template);
        when(exact.exists()).thenReturn(false);
        when(template.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/en/admin/roles/_/index.html")).thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void forwardsDynamicConsentDetailRouteToExportedTemplate() throws Exception {
        Resource exact = mock(Resource.class);
        Resource template = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/en/admin/consents/616263");
        when(resourceLoader.getResource("classpath:/static/en/admin/consents/616263/index.html"))
                .thenReturn(exact);
        when(resourceLoader.getResource("classpath:/static/en/admin/consents/_/index.html"))
                .thenReturn(template);
        when(exact.exists()).thenReturn(false);
        when(template.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/en/admin/consents/_/index.html"))
                .thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }
}
