package io.github.susimsek.springauthserversamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationMapper;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
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

    @Test
    void savesAndRemovesAuthorization() {
        OAuth2Authorization authorization = authorization();
        AuthorizationEntity entity = new AuthorizationEntity();
        when(authorizationMapper.toEntity(authorization, mapperSupport)).thenReturn(entity);

        DomainOAuth2AuthorizationService service =
                new DomainOAuth2AuthorizationService(
                        authorizationRepository,
                        registeredClientRepository,
                        authorizationMapper,
                        mapperSupport);
        service.save(authorization);
        service.remove(authorization);

        verify(authorizationRepository).save(entity);
        verify(authorizationRepository).deleteById("auth-1");
    }

    @Test
    void findsByIdAndToken() {
        AuthorizationEntity entity = new AuthorizationEntity();
        entity.setId("auth-1");
        entity.setRegisteredClientId("client-1");
        RegisteredClient client = registeredClient();
        OAuth2Authorization authorization = authorization();
        when(authorizationRepository.findById("auth-1")).thenReturn(Optional.of(entity));
        when(authorizationRepository.findOne(
                        org.mockito.ArgumentMatchers.<Specification<AuthorizationEntity>>any()))
                .thenReturn(
                        Optional.of(entity),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty());
        when(registeredClientRepository.findById("client-1")).thenReturn(client);
        when(authorizationMapper.toObject(entity, client, mapperSupport)).thenReturn(authorization);

        DomainOAuth2AuthorizationService service =
                new DomainOAuth2AuthorizationService(
                        authorizationRepository,
                        registeredClientRepository,
                        authorizationMapper,
                        mapperSupport);

        assertThat(service.findById("auth-1")).isSameAs(authorization);
        assertThat(service.findByToken("token", null)).isSameAs(authorization);
        assertThat(service.findByToken("token", new OAuth2TokenType("state"))).isNull();
        assertThat(service.findByToken("token", new OAuth2TokenType("code"))).isNull();
        assertThat(service.findByToken("token", OAuth2TokenType.ACCESS_TOKEN)).isNull();
        assertThat(service.findByToken("token", OAuth2TokenType.REFRESH_TOKEN)).isNull();
        assertThat(service.findByToken("token", new OAuth2TokenType("id_token"))).isNull();
        assertThat(service.findByToken("token", new OAuth2TokenType("user_code"))).isNull();
        assertThat(service.findByToken("token", new OAuth2TokenType("device_code"))).isNull();
        assertThat(service.findByToken("token", new OAuth2TokenType("unknown"))).isNull();
        assertThat(service.findById("missing")).isNull();
    }

    @Test
    void throwsWhenRegisteredClientIsMissing() {
        AuthorizationEntity entity = new AuthorizationEntity();
        entity.setRegisteredClientId("missing-client");
        when(authorizationRepository.findById("auth-1")).thenReturn(Optional.of(entity));
        when(registeredClientRepository.findById("missing-client")).thenReturn(null);

        DomainOAuth2AuthorizationService service =
                new DomainOAuth2AuthorizationService(
                        authorizationRepository,
                        registeredClientRepository,
                        authorizationMapper,
                        mapperSupport);

        assertThatThrownBy(() -> service.findById("auth-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Registered client not found: missing-client");
    }

    @Test
    @SuppressWarnings("unchecked")
    void byTokenSpecificationFactoryHandlesAllBranches() throws Exception {
        Method method =
                DomainOAuth2AuthorizationService.class.getDeclaredMethod(
                        "byToken", String.class, OAuth2TokenType.class);
        method.setAccessible(true);

        assertThat((Specification<AuthorizationEntity>) method.invoke(null, "token", null))
                .isNotNull();
        assertThat(
                        (Specification<AuthorizationEntity>)
                                method.invoke(null, "token", new OAuth2TokenType("state")))
                .isNotNull();
        assertThat(
                        (Specification<AuthorizationEntity>)
                                method.invoke(null, "token", new OAuth2TokenType("code")))
                .isNotNull();
        assertThat(
                        (Specification<AuthorizationEntity>)
                                method.invoke(null, "token", OAuth2TokenType.ACCESS_TOKEN))
                .isNotNull();
        assertThat(
                        (Specification<AuthorizationEntity>)
                                method.invoke(null, "token", OAuth2TokenType.REFRESH_TOKEN))
                .isNotNull();
        assertThat(
                        (Specification<AuthorizationEntity>)
                                method.invoke(null, "token", new OAuth2TokenType("id_token")))
                .isNotNull();
        assertThat(
                        (Specification<AuthorizationEntity>)
                                method.invoke(null, "token", new OAuth2TokenType("user_code")))
                .isNotNull();
        assertThat(
                        (Specification<AuthorizationEntity>)
                                method.invoke(null, "token", new OAuth2TokenType("device_code")))
                .isNotNull();
        assertThat(
                        (Specification<AuthorizationEntity>)
                                method.invoke(null, "token", new OAuth2TokenType("unknown")))
                .isNotNull();
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
