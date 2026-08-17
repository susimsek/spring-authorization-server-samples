package io.github.susimsek.springauthserversamples.web.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

class OidcUiLocalesFilterTest {

    private final LocaleResolver localeResolver = mock(LocaleResolver.class);
    private final OidcUiLocalesFilter filter = new OidcUiLocalesFilter(localeResolver);
    private final FilterChain filterChain = mock(FilterChain.class);

    @Test
    void usesFirstSupportedUiLocale() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        request.setParameter("ui_locales", "de tr-TR en");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(localeResolver).setLocale(request, response, Locale.forLanguageTag("tr"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void ignoresUnsupportedUiLocales() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        request.setParameter("ui_locales", "de fr");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(localeResolver, never()).setLocale(request, response, Locale.GERMAN);
        verify(filterChain).doFilter(request, response);
    }
}
