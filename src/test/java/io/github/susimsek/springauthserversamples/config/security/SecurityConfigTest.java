package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.security.LocalizedAccessDeniedHandler;
import io.github.susimsek.springauthserversamples.security.LocalizedAuthenticationEntryPoint;
import io.github.susimsek.springauthserversamples.service.SocialLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;

class SecurityConfigTest {

    private final SecurityConfig config =
            new SecurityConfig(
                    mock(LocalizedAuthenticationEntryPoint.class),
                    mock(LocalizedAccessDeniedHandler.class));

    @Test
    void createsPasswordEncoder() {
        assertThat(config.passwordEncoder()).isInstanceOf(DelegatingPasswordEncoder.class);
    }

    @Test
    void createsCompactUrlSafeStateValues() {
        String state = SecurityConfig.shortState();

        assertThat(state).hasSize(22).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void createsSecuritySupportBeansAndEmptySocialResolver() {
        assertThat(config.socialAuthorizedClientRepository()).isNotNull();
        assertThat(config.socialTokenResponseClient()).isNotNull();
        assertThat(config.browserSecurityContextRepository()).isNotNull();
        assertThat(config.authorizationServerSecurityContextRepository()).isNotNull();
        assertThat(config.authenticationEventPublisher(mock())).isNotNull();

        ObjectProvider<ClientRegistrationRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        var resolver =
                config.socialAuthorizationRequestResolver(provider, mock(SocialLoginService.class));
        assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
        assertThat(resolver.resolve(new MockHttpServletRequest(), "google")).isNull();
    }

    @Test
    void filtersDisabledProvidersAndDecoratesAllowedAuthorizationRequests() {
        ClientRegistration registration =
                ClientRegistration.withRegistrationId("google")
                        .clientId("client")
                        .clientSecret("secret")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .authorizationUri("https://example.test/authorize")
                        .tokenUri("https://example.test/token")
                        .scope("openid")
                        .build();
        SocialLoginService socialLoginService = mock(SocialLoginService.class);
        when(socialLoginService.isProviderLoginAllowed("google")).thenReturn(true);
        when(socialLoginService.requiresShortStateParameter("google")).thenReturn(true);
        when(socialLoginService.isLinkedInProvider("google")).thenReturn(false);
        ObjectProvider<ClientRegistrationRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable())
                .thenReturn(new InMemoryClientRegistrationRepository(registration));
        var resolver = config.socialAuthorizationRequestResolver(provider, socialLoginService);

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        var authorizationRequest = resolver.resolve(request, "google");
        assertThat(authorizationRequest).isNotNull();
        assertThat(authorizationRequest.getState()).hasSize(22);
        when(socialLoginService.isProviderLoginAllowed("google")).thenReturn(false);
        assertThat(resolver.resolve(request, "google")).isNull();
    }

    @Test
    void appliesProviderSpecificStateAndNonceRules() {
        ClientRegistration registration =
                ClientRegistration.withRegistrationId("linkedin")
                        .clientId("client")
                        .clientSecret("secret")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .authorizationUri("https://example.test/authorize")
                        .tokenUri("https://example.test/token")
                        .scope("openid")
                        .build();
        SocialLoginService socialLoginService = mock(SocialLoginService.class);
        when(socialLoginService.isProviderLoginAllowed("linkedin")).thenReturn(true);
        when(socialLoginService.requiresShortStateParameter("linkedin")).thenReturn(false);
        when(socialLoginService.isLinkedInProvider("linkedin")).thenReturn(true);
        ObjectProvider<ClientRegistrationRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable())
                .thenReturn(new InMemoryClientRegistrationRepository(registration));
        var resolver = config.socialAuthorizationRequestResolver(provider, socialLoginService);

        var authorizationRequest =
                resolver.resolve(
                        new MockHttpServletRequest("GET", "/oauth2/authorization/linkedin"),
                        "linkedin");

        assertThat(authorizationRequest).isNotNull();
        assertThat(authorizationRequest.getState()).isNotBlank();
        assertThat(authorizationRequest.getAdditionalParameters())
                .doesNotContainKey(OidcParameterNames.NONCE);
    }
}
