package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.mapper.AuthorizationConsentMapper;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DomainOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private final AuthorizationConsentRepository authorizationConsentRepository;
    private final AuthorizationConsentMapper authorizationConsentMapper;
    private final AuthorizationServerMapperSupport mapperSupport;

    @Override
    @Transactional
    public void save(OAuth2AuthorizationConsent authorizationConsent) {
        authorizationConsentRepository.save(
                authorizationConsentMapper.toEntity(authorizationConsent, mapperSupport));
    }

    @Override
    @Transactional
    public void remove(OAuth2AuthorizationConsent authorizationConsent) {
        authorizationConsentRepository.deleteByIdRegisteredClientIdAndIdPrincipalName(
                authorizationConsent.getRegisteredClientId(),
                authorizationConsent.getPrincipalName());
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        return authorizationConsentRepository
                .findByIdRegisteredClientIdAndIdPrincipalName(registeredClientId, principalName)
                .map(entity -> authorizationConsentMapper.toObject(entity, mapperSupport))
                .orElse(null);
    }
}
