package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.UserAvatarRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.security.AuthorizationEndpointErrorResponseHandler;
import io.github.susimsek.springauthserversamples.security.LocalizedOAuth2ErrorResponseHandler;
import io.github.susimsek.springauthserversamples.security.OAuth2KeyJwkSource;
import io.github.susimsek.springauthserversamples.service.OAuth2KeyService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

class AuthorizationServerConfigTest {

    private final ApplicationProperties applicationProperties = applicationProperties();

    private final AuthorizationServerConfig config =
            new AuthorizationServerConfig(
                    applicationProperties,
                    mock(AuthorizationEndpointErrorResponseHandler.class),
                    mock(LocalizedOAuth2ErrorResponseHandler.class));

    @Test
    void createsAuthorizationServerSettingsFromProperties() {
        AuthorizationServerSettings settings = config.authorizationServerSettings();

        assertThat(settings.getIssuer()).isEqualTo("https://issuer.example");
    }

    @Test
    void createsDatabaseBackedJwkSource() {
        OAuth2KeyService oauth2KeyService = mock(OAuth2KeyService.class);

        var jwkSource = config.jwkSource(oauth2KeyService);

        assertThat(jwkSource).isInstanceOf(OAuth2KeyJwkSource.class);
    }

    @Test
    void createsJwtDecoder() {
        OAuth2KeyService oauth2KeyService = mock(OAuth2KeyService.class);
        var jwkSource = config.jwkSource(oauth2KeyService);

        JwtDecoder jwtDecoder = config.jwtDecoder(jwkSource);

        assertThat(jwtDecoder).isNotNull();
    }

    @Test
    void addsProfilePictureAndAdminRolesToAccessToken() {
        UserRepository userRepository = mock(UserRepository.class);
        UserAvatarRepository avatarRepository = mock(UserAvatarRepository.class);
        UserEntity user = new UserEntity();
        user.setId(42L);
        UserAvatarRepository.AvatarVersion avatar = mock(UserAvatarRepository.AvatarVersion.class);
        when(avatar.getPublicId()).thenReturn("avatar-id");
        when(avatar.getUpdatedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(avatarRepository.findVersionByUserId(42L)).thenReturn(Optional.of(avatar));
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder();

        config.jwtTokenCustomizer(userRepository, avatarRepository)
                .customize(
                        jwtContext(
                                claims,
                                OAuth2TokenType.ACCESS_TOKEN,
                                AuthorizationGrantType.AUTHORIZATION_CODE,
                                "admin-console",
                                Set.of("profile")));

        assertThat(claims.build().getClaims())
                .containsEntry(
                        "picture", "https://issuer.example/avatars/avatar-id?v=1767225600000")
                .containsEntry("roles", List.of("ROLE_ADMIN", "ROLE_USER"));
        verify(userRepository).findByUsername("admin");
        verify(avatarRepository).findVersionByUserId(42L);
    }

    @Test
    void doesNotQueryAvatarForTokensOutsideUserProfileFlows() {
        UserRepository userRepository = mock(UserRepository.class);
        UserAvatarRepository avatarRepository = mock(UserAvatarRepository.class);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder();

        config.jwtTokenCustomizer(userRepository, avatarRepository)
                .customize(
                        jwtContext(
                                claims,
                                new OAuth2TokenType("refresh_token"),
                                AuthorizationGrantType.CLIENT_CREDENTIALS,
                                "other-client",
                                Set.of()));

        claims.claim("sub", "admin");
        assertThat(claims.build().getClaims()).doesNotContainKeys("picture", "roles");
        verify(userRepository, org.mockito.Mockito.never())
                .findByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    private static JwtEncodingContext jwtContext(
            JwtClaimsSet.Builder claims,
            OAuth2TokenType tokenType,
            AuthorizationGrantType grantType,
            String clientId,
            Set<String> scopes) {
        return JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), claims)
                .registeredClient(
                        RegisteredClient.withId("client-id")
                                .clientId(clientId)
                                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                .redirectUri("https://client.example/callback")
                                .build())
                .principal(
                        new UsernamePasswordAuthenticationToken(
                                "admin",
                                "n/a",
                                List.of(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .authorizedScopes(scopes)
                .tokenType(tokenType)
                .authorizationGrantType(grantType)
                .build();
    }

    private static ApplicationProperties applicationProperties() {
        return new ApplicationProperties(
                new ApplicationProperties.Cache(
                        new ApplicationProperties.Caffeine(
                                java.time.Duration.ofHours(1), 500, 1000)),
                new ApplicationProperties.Session("0 * * * * *"),
                new ApplicationProperties.AuthorizationServer("https://issuer.example"));
    }
}
