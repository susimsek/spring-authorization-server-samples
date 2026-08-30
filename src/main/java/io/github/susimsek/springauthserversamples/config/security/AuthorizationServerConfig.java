package io.github.susimsek.springauthserversamples.config.security;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.repository.UserAvatarRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.security.AuthorizationEndpointErrorResponseHandler;
import io.github.susimsek.springauthserversamples.security.LocalizedOAuth2ErrorResponseHandler;
import io.github.susimsek.springauthserversamples.security.OAuth2KeyJwkSource;
import io.github.susimsek.springauthserversamples.service.OAuth2KeyService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            OAuth2TokenGenerator<OAuth2Token> tokenGenerator,
            RegisteredClientRepository registeredClientRepository,
            @Qualifier("authorizationServerSecurityContextRepository")
                    SecurityContextRepository securityContextRepository) {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .securityContext(
                        securityContext ->
                                securityContext
                                        .securityContextRepository(securityContextRepository)
                                        .requireExplicitSave(false))
                .with(
                        authorizationServerConfigurer,
                        authorizationServer ->
                                authorizationServer
                                        .tokenGenerator(tokenGenerator)
                                        .oidc(Customizer.withDefaults())
                                        .authorizationEndpoint(
                                                authorizationEndpoint ->
                                                        authorizationEndpoint
                                                                .authorizationRequestConverter(
                                                                        new DefaultClientScopesAuthorizationRequestConverter(
                                                                                registeredClientRepository))
                                                                .consentPage("/consent")
                                                                .errorResponseHandler(
                                                                        authorizationEndpointErrorResponseHandler))
                                        .clientAuthentication(
                                                clientAuthentication ->
                                                        clientAuthentication
                                                                .authenticationConverter(
                                                                        new AdminConsoleRefreshClientAuthenticationConverter())
                                                                .authenticationProvider(
                                                                        new AdminConsoleRefreshClientAuthenticationProvider(
                                                                                registeredClientRepository))
                                                                .errorResponseHandler(
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
                                                        tokenEndpoint
                                                                .accessTokenRequestConverter(
                                                                        new DefaultClientScopesClientCredentialsConverter())
                                                                .errorResponseHandler(
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
    @Primary
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    OAuth2TokenGenerator<OAuth2Token> tokenGenerator(
            JwtEncoder jwtEncoder, OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer) {
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(jwtTokenCustomizer);

        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator,
                new OAuth2AccessTokenGenerator(),
                new AdminConsoleRefreshTokenGenerator());
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(
            UserRepository userRepository, UserAvatarRepository userAvatarRepository) {
        return context -> {
            if (isUserProfileToken(context)) {
                userRepository
                        .findByUsername(context.getPrincipal().getName())
                        .flatMap(user -> userAvatarRepository.findVersionByUserId(user.getId()))
                        .ifPresent(
                                avatar ->
                                        context.getClaims()
                                                .claim(
                                                        "picture",
                                                        applicationProperties
                                                                        .authorizationServer()
                                                                        .issuer()
                                                                + "/avatars/"
                                                                + avatar.getPublicId()
                                                                + "?v="
                                                                + avatar.getUpdatedAt()
                                                                        .toEpochMilli()));
            }

            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())
                    && "admin-console".equals(context.getRegisteredClient().getClientId())) {
                context.getClaims()
                        .claim(
                                "roles",
                                context.getPrincipal().getAuthorities().stream()
                                        .map(authority -> authority.getAuthority())
                                        .sorted()
                                        .collect(Collectors.toList()));
            }

            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())
                    && Set.of("admin-console", "account-console")
                            .contains(context.getRegisteredClient().getClientId())) {
                currentSessionId()
                        .ifPresent(sessionId -> context.getClaims().claim("sid", sessionId));
            }
        };
    }

    private static boolean isUserProfileToken(JwtEncodingContext context) {
        return context.getAuthorizedScopes().contains("profile")
                && (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())
                        || OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue()))
                && (AuthorizationGrantType.AUTHORIZATION_CODE.equals(
                                context.getAuthorizationGrantType())
                        || AuthorizationGrantType.REFRESH_TOKEN.equals(
                                context.getAuthorizationGrantType()));
    }

    private static java.util.Optional<String> currentSessionId() {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            return java.util.Optional.empty();
        }
        var session = attributes.getRequest().getSession(false);
        if (session == null) {
            return java.util.Optional.empty();
        }
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(session.getId().getBytes(StandardCharsets.US_ASCII));
            return java.util.Optional.of(
                    Base64.getUrlEncoder().withoutPadding().encodeToString(digest));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for OIDC session identifiers", ex);
        }
    }
}
