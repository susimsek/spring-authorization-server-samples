package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationMapper;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return authorizationRepository
                .findOne(byToken(token, tokenType))
                .map(this::toObject)
                .orElse(null);
    }

    private static Specification<AuthorizationEntity> byToken(
            String token, OAuth2TokenType tokenType) {
        if (tokenType == null) {
            return stateEquals(token)
                    .or(authorizationCodeEquals(token))
                    .or(accessTokenEquals(token))
                    .or(refreshTokenEquals(token))
                    .or(oidcIdTokenEquals(token))
                    .or(userCodeEquals(token))
                    .or(deviceCodeEquals(token));
        }
        return switch (tokenType.getValue()) {
            case "state" -> stateEquals(token);
            case "code" -> authorizationCodeEquals(token);
            case "access_token" -> accessTokenEquals(token);
            case "refresh_token" -> refreshTokenEquals(token);
            case "id_token" -> oidcIdTokenEquals(token);
            case "user_code" -> userCodeEquals(token);
            case "device_code" -> deviceCodeEquals(token);
            default -> (root, query, criteriaBuilder) -> criteriaBuilder.disjunction();
        };
    }

    private static Specification<AuthorizationEntity> stateEquals(String token) {
        return fieldEquals("state", token);
    }

    private static Specification<AuthorizationEntity> authorizationCodeEquals(String token) {
        return fieldEquals("authorizationCodeValue", token);
    }

    private static Specification<AuthorizationEntity> accessTokenEquals(String token) {
        return fieldEquals("accessTokenValue", token);
    }

    private static Specification<AuthorizationEntity> refreshTokenEquals(String token) {
        return fieldEquals("refreshTokenValue", token);
    }

    private static Specification<AuthorizationEntity> oidcIdTokenEquals(String token) {
        return fieldEquals("oidcIdTokenValue", token);
    }

    private static Specification<AuthorizationEntity> userCodeEquals(String token) {
        return fieldEquals("userCodeValue", token);
    }

    private static Specification<AuthorizationEntity> deviceCodeEquals(String token) {
        return fieldEquals("deviceCodeValue", token);
    }

    private static Specification<AuthorizationEntity> fieldEquals(String fieldName, String token) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(fieldName), token);
    }

    private OAuth2Authorization toObject(AuthorizationEntity entity) {
        RegisteredClient registeredClient =
                registeredClientRepository.findById(entity.getRegisteredClientId());
        if (registeredClient == null) {
            throw new IllegalStateException(
                    "Registered client not found: " + entity.getRegisteredClientId());
        }
        return authorizationMapper.toObject(entity, registeredClient, mapperSupport);
    }
}
