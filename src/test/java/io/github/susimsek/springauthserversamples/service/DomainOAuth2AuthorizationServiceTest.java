package io.github.susimsek.springauthserversamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationMapper;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@ExtendWith(MockitoExtension.class)
class DomainOAuth2AuthorizationServiceTest {

    @Mock private AuthorizationRepository authorizationRepository;
    @Mock private RegisteredClientRepository registeredClientRepository;
    @Mock private AuthorizationMapper authorizationMapper;
    @Mock private AuthorizationServerMapperSupport mapperSupport;

    private DomainOAuth2AuthorizationService service;

    @BeforeEach
    void setUp() {
        service =
                new DomainOAuth2AuthorizationService(
                        authorizationRepository,
                        registeredClientRepository,
                        authorizationMapper,
                        mapperSupport);
    }

    @Test
    void savesAndRemovesAuthorization() {
        OAuth2Authorization authorization = authorization();
        AuthorizationEntity entity = new AuthorizationEntity();
        when(authorizationMapper.toEntity(authorization, mapperSupport)).thenReturn(entity);
        when(authorizationRepository.findById("auth-1")).thenReturn(Optional.empty());

        service.save(authorization);
        service.remove(authorization);

        verify(authorizationRepository).save(entity);
        verify(authorizationRepository).deleteById("auth-1");
    }

    @Test
    void findsByIdAndTokenTypes() {
        AuthorizationEntity entity = entity();
        RegisteredClient client = registeredClient();
        OAuth2Authorization authorization = authorization();

        when(registeredClientRepository.findById("client-1")).thenReturn(client);
        when(authorizationMapper.toObject(entity, client, mapperSupport)).thenReturn(authorization);
        when(authorizationRepository.findById("auth-1")).thenReturn(Optional.of(entity));
        when(authorizationRepository.findByToken("token")).thenReturn(Optional.of(entity));
        when(authorizationRepository.findByState("token")).thenReturn(Optional.of(entity));
        when(authorizationRepository.findByAuthorizationCodeValue("token"))
                .thenReturn(Optional.of(entity));
        when(authorizationRepository.findByAccessTokenValue("token"))
                .thenReturn(Optional.of(entity));
        when(authorizationRepository.findByRefreshTokenValue("token"))
                .thenReturn(Optional.of(entity));
        when(authorizationRepository.findByOidcIdTokenValue("token"))
                .thenReturn(Optional.of(entity));
        when(authorizationRepository.findByUserCodeValue("token")).thenReturn(Optional.of(entity));
        when(authorizationRepository.findByDeviceCodeValue("token"))
                .thenReturn(Optional.of(entity));

        assertThat(service.findById("auth-1")).isSameAs(authorization);
        assertThat(service.findByToken("token", null)).isSameAs(authorization);
        assertThat(service.findByToken("token", new OAuth2TokenType("state")))
                .isSameAs(authorization);
        assertThat(service.findByToken("token", new OAuth2TokenType("code")))
                .isSameAs(authorization);
        assertThat(service.findByToken("token", OAuth2TokenType.ACCESS_TOKEN))
                .isSameAs(authorization);
        assertThat(service.findByToken("token", OAuth2TokenType.REFRESH_TOKEN))
                .isSameAs(authorization);
        assertThat(service.findByToken("token", new OAuth2TokenType("id_token")))
                .isSameAs(authorization);
        assertThat(service.findByToken("token", new OAuth2TokenType("user_code")))
                .isSameAs(authorization);
        assertThat(service.findByToken("token", new OAuth2TokenType("device_code")))
                .isSameAs(authorization);
        assertThat(service.findByToken("token", new OAuth2TokenType("unknown"))).isNull();
        assertThat(service.findById("missing")).isNull();
    }

    @Test
    void rejectsBlankToken() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.findByToken(" ", OAuth2TokenType.ACCESS_TOKEN))
                .withMessage("token cannot be empty");
    }

    @Test
    void throwsWhenRegisteredClientIsMissing() {
        AuthorizationEntity entity = entity();
        when(authorizationRepository.findById("auth-1")).thenReturn(Optional.of(entity));
        when(registeredClientRepository.findById("client-1")).thenReturn(null);

        assertThatThrownBy(() -> service.findById("auth-1"))
                .isInstanceOf(DataRetrievalFailureException.class)
                .hasMessage("Registered client not found: client-1");
    }

    private static AuthorizationEntity entity() {
        AuthorizationEntity entity = new AuthorizationEntity();
        entity.setId("auth-1");
        entity.setRegisteredClientId("client-1");
        return entity;
    }

    private static OAuth2Authorization authorization() {
        return OAuth2Authorization.withRegisteredClient(registeredClient())
                .id("auth-1")
                .principalName("admin")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
    }

    private static RegisteredClient registeredClient() {
        return RegisteredClient.withId("client-1")
                .clientId("demo-client")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("openid")
                .build();
    }
}
