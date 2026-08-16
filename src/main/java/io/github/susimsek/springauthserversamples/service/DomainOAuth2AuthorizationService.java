package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationMapper;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class DomainOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final AuthorizationRepository authorizationRepository;
    private final RegisteredClientRepository registeredClientRepository;
    private final AuthorizationMapper authorizationMapper;
    private final AuthorizationServerMapperSupport mapperSupport;

    @Override
    @Transactional
    public void save(OAuth2Authorization authorization) {
        authorizationRepository.save(authorizationMapper.toEntity(authorization, mapperSupport));
    }

    @Override
    @Transactional
    public void remove(OAuth2Authorization authorization) {
        authorizationRepository.deleteById(authorization.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2Authorization findById(String id) {
        return authorizationRepository.findById(id).map(this::toObject).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");

        Optional<AuthorizationEntity> authorization =
                tokenType == null
                        ? authorizationRepository.findByToken(token)
                        : findByTokenType(token, tokenType);

        return authorization.map(this::toObject).orElse(null);
    }

    private Optional<AuthorizationEntity> findByTokenType(String token, OAuth2TokenType tokenType) {
        return switch (tokenType.getValue()) {
            case OAuth2ParameterNames.STATE -> authorizationRepository.findByState(token);
            case OAuth2ParameterNames.CODE ->
                    authorizationRepository.findByAuthorizationCodeValue(token);
            case OAuth2ParameterNames.ACCESS_TOKEN ->
                    authorizationRepository.findByAccessTokenValue(token);
            case OAuth2ParameterNames.REFRESH_TOKEN ->
                    authorizationRepository.findByRefreshTokenValue(token);
            case OidcParameterNames.ID_TOKEN ->
                    authorizationRepository.findByOidcIdTokenValue(token);
            case OAuth2ParameterNames.USER_CODE ->
                    authorizationRepository.findByUserCodeValue(token);
            case OAuth2ParameterNames.DEVICE_CODE ->
                    authorizationRepository.findByDeviceCodeValue(token);
            default -> Optional.empty();
        };
    }

    private OAuth2Authorization toObject(AuthorizationEntity entity) {
        RegisteredClient registeredClient =
                registeredClientRepository.findById(entity.getRegisteredClientId());
        if (registeredClient == null) {
            throw new DataRetrievalFailureException(
                    "Registered client not found: " + entity.getRegisteredClientId());
        }
        return authorizationMapper.toObject(entity, registeredClient, mapperSupport);
    }
}
