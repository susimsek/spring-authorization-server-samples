package io.github.susimsek.springauthserversamples.config.security;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.security.AuthorizationEndpointErrorResponseHandler;
import io.github.susimsek.springauthserversamples.security.LocalizedOAuth2ErrorResponseHandler;
import io.github.susimsek.springauthserversamples.security.OAuth2KeyJwkSource;
import io.github.susimsek.springauthserversamples.service.OAuth2KeyService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    private static final MediaTypeRequestMatcher HTML_REQUEST_MATCHER = htmlRequestMatcher();

    private final ApplicationProperties applicationProperties;
    private final AuthorizationEndpointErrorResponseHandler
            authorizationEndpointErrorResponseHandler;
    private final LocalizedOAuth2ErrorResponseHandler localizedOAuth2ErrorResponseHandler;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(
                        authorizationServerConfigurer,
                        authorizationServer ->
                                authorizationServer
                                        .oidc(Customizer.withDefaults())
                                        .authorizationEndpoint(
                                                authorizationEndpoint ->
                                                        authorizationEndpoint
                                                                .consentPage("/consent")
                                                                .errorResponseHandler(
                                                                        authorizationEndpointErrorResponseHandler))
                                        .clientAuthentication(
                                                clientAuthentication ->
                                                        clientAuthentication.errorResponseHandler(
                                                                localizedOAuth2ErrorResponseHandler))
                                        .pushedAuthorizationRequestEndpoint(
                                                pushedAuthorizationRequestEndpoint ->
                                                        pushedAuthorizationRequestEndpoint
                                                                .errorResponseHandler(
                                                                        localizedOAuth2ErrorResponseHandler))
                                        .deviceAuthorizationEndpoint(
                                                deviceAuthorizationEndpoint ->
                                                        deviceAuthorizationEndpoint
                                                                .errorResponseHandler(
                                                                        localizedOAuth2ErrorResponseHandler))
                                        .tokenEndpoint(
                                                tokenEndpoint ->
                                                        tokenEndpoint.errorResponseHandler(
                                                                localizedOAuth2ErrorResponseHandler))
                                        .tokenIntrospectionEndpoint(
                                                tokenIntrospectionEndpoint ->
                                                        tokenIntrospectionEndpoint
                                                                .errorResponseHandler(
                                                                        localizedOAuth2ErrorResponseHandler))
                                        .tokenRevocationEndpoint(
                                                tokenRevocationEndpoint ->
                                                        tokenRevocationEndpoint
                                                                .errorResponseHandler(
                                                                        localizedOAuth2ErrorResponseHandler)))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(
                        resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .exceptionHandling(
                        exceptions ->
                                exceptions.defaultAuthenticationEntryPointFor(
                                        new LoginUrlAuthenticationEntryPoint("/login"),
                                        HTML_REQUEST_MATCHER));

        return http.build();
    }

    private static MediaTypeRequestMatcher htmlRequestMatcher() {
        MediaTypeRequestMatcher requestMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        requestMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return requestMatcher;
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(applicationProperties.authorizationServer().issuer())
                .build();
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(OAuth2KeyService oauth2KeyService) {
        return new OAuth2KeyJwkSource(oauth2KeyService);
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
}
