package io.github.susimsek.springauthserversamples.config.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.security.Jwks;
import io.github.susimsek.springauthserversamples.security.LocalizedAccessDeniedHandler;
import io.github.susimsek.springauthserversamples.security.LocalizedAuthenticationEntryPoint;
import io.github.susimsek.springauthserversamples.security.LocalizedOAuth2ErrorResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    private static final MediaTypeRequestMatcher HTML_REQUEST_MATCHER =
            new MediaTypeRequestMatcher(MediaType.TEXT_HTML);

    private static final NegatedRequestMatcher NON_HTML_REQUEST_MATCHER =
            new NegatedRequestMatcher(HTML_REQUEST_MATCHER);

    private final ApplicationProperties applicationProperties;
    private final LocalizedOAuth2ErrorResponseHandler localizedOAuth2ErrorResponseHandler;
    private final LocalizedAuthenticationEntryPoint localizedAuthenticationEntryPoint;
    private final LocalizedAccessDeniedHandler localizedAccessDeniedHandler;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) {

        http.securityMatcher(
                "/oauth2/**", "/.well-known/**", "/connect/**", "/userinfo", "/userinfo/**");

        http.oauth2AuthorizationServer(
                authorizationServer ->
                        authorizationServer
                                .oidc(Customizer.withDefaults())
                                .clientAuthentication(
                                        clientAuthentication ->
                                                clientAuthentication.errorResponseHandler(
                                                        localizedOAuth2ErrorResponseHandler))
                                .tokenEndpoint(
                                        tokenEndpoint ->
                                                tokenEndpoint.errorResponseHandler(
                                                        localizedOAuth2ErrorResponseHandler))
                                .tokenIntrospectionEndpoint(
                                        tokenIntrospectionEndpoint ->
                                                tokenIntrospectionEndpoint.errorResponseHandler(
                                                        localizedOAuth2ErrorResponseHandler))
                                .tokenRevocationEndpoint(
                                        tokenRevocationEndpoint ->
                                                tokenRevocationEndpoint.errorResponseHandler(
                                                        localizedOAuth2ErrorResponseHandler)));

        http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());

        http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        http.exceptionHandling(
                exceptions ->
                        exceptions
                                .defaultAuthenticationEntryPointFor(
                                        new LoginUrlAuthenticationEntryPoint("/login"),
                                        HTML_REQUEST_MATCHER)
                                .defaultAuthenticationEntryPointFor(
                                        localizedAuthenticationEntryPoint, NON_HTML_REQUEST_MATCHER)
                                .accessDeniedHandler(localizedAccessDeniedHandler));

        return http.build();
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(applicationProperties.getAuthorizationServer().getIssuer())
                .build();
    }

    @Bean
    JWKSource<SecurityContext> jwkSource() {
        var jwkProperties = applicationProperties.getAuthorizationServer().getJwk();
        RSAKey rsaKey =
                Jwks.loadRsa(
                        jwkProperties.getPublicKey(),
                        jwkProperties.getPrivateKey(),
                        jwkProperties.getKeyId());
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
}
