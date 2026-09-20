package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.config.security.SocialLoginSecretCipher;
import io.github.susimsek.springauthserversamples.domain.SocialIdentityEntity;
import io.github.susimsek.springauthserversamples.dto.account.SocialProviderTokenDTO;
import io.github.susimsek.springauthserversamples.repository.SocialIdentityRepository;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores and exposes external provider tokens according to provider settings. */
@Service
@RequiredArgsConstructor
public class SocialTokenService {

    private final SocialIdentityRepository socialIdentityRepository;
    private final SocialProviderSettingsService providerSettingsService;
    private final SocialLoginSecretCipher secretCipher;

    @Transactional
    public void store(
            String username, String providerValue, OAuth2AuthorizedClient authorizedClient) {
        if (authorizedClient == null) {
            return;
        }
        SocialProviderSettingsService.ProviderCredentials provider =
                providerSettingsService.provider(providerValue);
        if (provider == null) {
            return;
        }
        SocialIdentityEntity identity = identity(username, provider.registrationId());
        if (!provider.storeTokens()) {
            clear(identity);
        } else {
            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
            OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
            if (accessToken == null) {
                clear(identity);
                socialIdentityRepository.save(identity);
                return;
            }
            identity.setAccessTokenEncrypted(encrypt(accessToken.getTokenValue()));
            identity.setRefreshTokenEncrypted(
                    refreshToken == null ? null : encrypt(refreshToken.getTokenValue()));
            identity.setAccessTokenExpiresAt(accessToken.getExpiresAt());
            identity.setTokenType(
                    accessToken.getTokenType() == null
                            ? null
                            : accessToken.getTokenType().getValue());
            identity.setTokenScopes(
                    accessToken.getScopes() == null
                            ? null
                            : accessToken.getScopes().stream()
                                    .sorted()
                                    .collect(Collectors.joining(" ")));
        }
        socialIdentityRepository.save(identity);
    }

    @Transactional(readOnly = true)
    public SocialProviderTokenDTO read(String username, String providerValue) {
        SocialProviderSettingsService.ProviderCredentials provider =
                providerSettingsService.provider(providerValue);
        if (provider == null) {
            throw ApiException.notFound("Social provider not found");
        }
        if (!provider.storedTokensReadable()) {
            throw ApiException.forbidden(
                    ApiErrorCode.FORBIDDEN, "Stored tokens are not readable for this provider");
        }
        SocialIdentityEntity identity = identity(username, provider.registrationId());
        if (identity.getAccessTokenEncrypted() == null
                || identity.getAccessTokenEncrypted().isBlank()) {
            throw ApiException.notFound("No stored token is available for this provider");
        }
        String refreshToken =
                identity.getRefreshTokenEncrypted() == null
                        ? null
                        : decrypt(identity.getRefreshTokenEncrypted());
        return new SocialProviderTokenDTO(
                provider.alias(),
                identity.getTokenType(),
                decrypt(identity.getAccessTokenEncrypted()),
                refreshToken,
                identity.getAccessTokenExpiresAt(),
                identity.getTokenScopes());
    }

    private SocialIdentityEntity identity(String username, String provider) {
        return socialIdentityRepository
                .findAllByUserUsernameAndProvider(username, provider)
                .stream()
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Social provider is not linked"));
    }

    private void clear(SocialIdentityEntity identity) {
        identity.setAccessTokenEncrypted(null);
        identity.setRefreshTokenEncrypted(null);
        identity.setAccessTokenExpiresAt(null);
        identity.setTokenType(null);
        identity.setTokenScopes(null);
    }

    private String encrypt(String value) {
        try {
            return secretCipher.encrypt(value);
        } catch (IllegalStateException exception) {
            throw ApiException.serverError(
                    ApiErrorCode.INTERNAL_ERROR,
                    "The external provider token could not be stored",
                    exception);
        }
    }

    private String decrypt(String value) {
        try {
            return secretCipher.decrypt(value);
        } catch (IllegalStateException exception) {
            throw ApiException.serverError(
                    ApiErrorCode.INTERNAL_ERROR,
                    "The external provider token could not be read",
                    exception);
        }
    }
}
