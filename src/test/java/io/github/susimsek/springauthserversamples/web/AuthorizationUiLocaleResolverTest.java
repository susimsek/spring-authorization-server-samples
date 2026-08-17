package io.github.susimsek.springauthserversamples.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

class AuthorizationUiLocaleResolverTest {

    private final AuthorizationUiLocaleResolver resolver = new AuthorizationUiLocaleResolver();

    @Test
    void prefersLocaleCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthorizationUiLocaleResolver.LOCALE_COOKIE_NAME, "tr"));
        request.addPreferredLocale(Locale.ENGLISH);

        assertThat(resolver.resolve(request, new MockHttpServletResponse())).isEqualTo("tr");
    }

    @Test
    void resolvesUiLocalesFromSavedAuthorizationRequest() {
        MockHttpServletRequest authorizationRequest =
                new MockHttpServletRequest("GET", "/oauth2/authorize");
        authorizationRequest.setQueryString(
                "response_type=code&client_id=demo-client&ui_locales=de%20tr-TR%20en");
        MockHttpServletResponse authorizationResponse = new MockHttpServletResponse();
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.saveRequest(authorizationRequest, authorizationResponse);

        MockHttpServletRequest loginRequest = new MockHttpServletRequest();
        loginRequest.setSession(authorizationRequest.getSession());
        loginRequest.addPreferredLocale(Locale.ENGLISH);

        assertThat(resolver.resolve(loginRequest, new MockHttpServletResponse())).isEqualTo("tr");
    }

    @Test
    void fallsBackToAcceptLanguage() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(Locale.forLanguageTag("tr-TR"));

        assertThat(resolver.resolve(request, new MockHttpServletResponse())).isEqualTo("tr");
    }

    @Test
    void fallsBackToEnglishForUnsupportedLocale() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(Locale.GERMAN);

        assertThat(resolver.resolve(request, new MockHttpServletResponse())).isEqualTo("en");
    }
}
