package io.github.susimsek.springauthserversamples.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

class SpaFilterTest {

    private final AuthorizationUiLocaleResolver localeResolver =
            mock(AuthorizationUiLocaleResolver.class);
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
    void forwardsLocaleLessRouteToResolvedLocalizedPage() throws Exception {
        Resource english = mock(Resource.class);
        Resource turkish = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/login");
        when(localeResolver.isSupportedLocale("login")).thenReturn(false);
        when(localeResolver.supportedLocales()).thenReturn(Set.of("en", "tr"));

        when(resourceLoader.getResource("classpath:/static/en/login/index.html"))
                .thenReturn(english);
        when(resourceLoader.getResource("classpath:/static/tr/login/index.html"))
                .thenReturn(turkish);
        when(english.exists()).thenReturn(true);

        when(localeResolver.resolve(request, response)).thenReturn("tr");
        when(request.getRequestDispatcher("/tr/login/index.html")).thenReturn(dispatcher);
        when(turkish.exists()).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void forwardsAlreadyLocalizedRouteDirectly() throws Exception {
        Resource resource = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/en/login/");
        when(localeResolver.isSupportedLocale("en")).thenReturn(true);
        when(resourceLoader.getResource("classpath:/static/en/login/index.html"))
                .thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(request.getRequestDispatcher("/en/login/index.html")).thenReturn(dispatcher);

        filter.doFilterInternal(request, response, filterChain);

        verify(dispatcher).forward(request, response);
        verify(localeResolver, never()).resolve(request, response);
    }

    @Test
    void continuesChainWhenNoLocalizedStaticPageExists() throws Exception {
        Resource english = mock(Resource.class);
        Resource turkish = mock(Resource.class);

        when(request.getRequestURI()).thenReturn("/oauth2/authorize");
        when(localeResolver.isSupportedLocale("oauth2")).thenReturn(false);
        when(localeResolver.supportedLocales()).thenReturn(Set.of("en", "tr"));
        when(resourceLoader.getResource("classpath:/static/en/oauth2/authorize/index.html"))
                .thenReturn(english);
        when(resourceLoader.getResource("classpath:/static/tr/oauth2/authorize/index.html"))
                .thenReturn(turkish);
        when(english.exists()).thenReturn(false);
        when(turkish.exists()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(localeResolver, never()).resolve(request, response);
    }

    @Test
    void ignoresAssetsAndNonGetRequests() throws Exception {
        when(request.getRequestURI()).thenReturn("/_next/static/app.js");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
