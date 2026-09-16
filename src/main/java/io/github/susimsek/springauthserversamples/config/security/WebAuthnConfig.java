package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.webauthn.api.AttestationConveyancePreference;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;
import org.springframework.security.web.webauthn.authentication.HttpSessionPublicKeyCredentialRequestOptionsRepository;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationProvider;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;

@Configuration(proxyBeanMethods = false)
public class WebAuthnConfig {

    @Bean
    PublicKeyCredentialUserEntityRepository webAuthnUserEntityRepository(
            UserRepository userRepository) {
        return new WebAuthnUserEntityRepository(userRepository);
    }

    @Bean
    UserCredentialRepository webAuthnUserCredentialRepository(JdbcOperations jdbcOperations) {
        return new JdbcUserCredentialRepository(jdbcOperations);
    }

    @Bean
    PublicKeyCredentialRequestOptionsRepository webAuthnRequestOptionsRepository() {
        return new HttpSessionPublicKeyCredentialRequestOptionsRepository();
    }

    @Bean
    WebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations(
            PublicKeyCredentialUserEntityRepository userEntities,
            UserCredentialRepository userCredentials,
            ApplicationProperties applicationProperties) {
        URI issuer = URI.create(applicationProperties.authorizationServer().issuer());
        ApplicationProperties.WebAuthn policy = applicationProperties.webAuthn();
        String origin = issuer.getScheme() + "://" + issuer.getRawAuthority();
        String configuredRpId = policy.rpId() == null ? "" : policy.rpId().trim();
        String rpId = configuredRpId.isBlank() ? issuer.getHost() : configuredRpId;
        String configuredOrigins = policy.allowedOrigins() == null ? "" : policy.allowedOrigins();
        Set<String> allowedOrigins =
                configuredOrigins.isBlank()
                        ? Set.of(origin)
                        : Arrays.stream(configuredOrigins.split(","))
                                .map(String::trim)
                                .filter(value -> !value.isBlank())
                                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Webauthn4JRelyingPartyOperations operations =
                new Webauthn4JRelyingPartyOperations(
                        userEntities,
                        userCredentials,
                        PublicKeyCredentialRpEntity.builder()
                                .id(rpId)
                                .name(
                                        policy.rpName() == null || policy.rpName().isBlank()
                                                ? "Spring Authorization Server Samples"
                                                : policy.rpName().trim())
                                .build(),
                        allowedOrigins);
        operations.setCustomizeCreationOptions(
                options ->
                        options.timeout(Duration.ofSeconds(Math.max(1, policy.timeoutSeconds())))
                                .authenticatorSelection(
                                        org.springframework.security.web.webauthn.api
                                                .AuthenticatorSelectionCriteria.builder()
                                                .residentKey(
                                                        ResidentKeyRequirement.valueOf(
                                                                policy.residentKey().toUpperCase()))
                                                .userVerification(
                                                        userVerification(policy.userVerification()))
                                                .build())
                                .attestation(attestation(policy.attestation())));
        operations.setCustomizeRequestOptions(
                options ->
                        options.timeout(Duration.ofSeconds(Math.max(1, policy.timeoutSeconds())))
                                .userVerification(userVerification(policy.userVerification())));
        return operations;
    }

    private static UserVerificationRequirement userVerification(String value) {
        return switch (value == null ? "" : value.toUpperCase(Locale.ROOT)) {
            case "DISCOURAGED" -> UserVerificationRequirement.DISCOURAGED;
            case "PREFERRED" -> UserVerificationRequirement.PREFERRED;
            default -> UserVerificationRequirement.REQUIRED;
        };
    }

    private static AttestationConveyancePreference attestation(String value) {
        return switch (value == null ? "" : value.toUpperCase(Locale.ROOT)) {
            case "DIRECT" -> AttestationConveyancePreference.DIRECT;
            case "INDIRECT" -> AttestationConveyancePreference.INDIRECT;
            case "ENTERPRISE" -> AttestationConveyancePreference.ENTERPRISE;
            default -> AttestationConveyancePreference.NONE;
        };
    }

    @Bean
    AuthenticationManager webAuthnAuthenticationManager(
            WebAuthnRelyingPartyOperations relyingPartyOperations,
            UserDetailsService userDetailsService) {
        return new ProviderManager(
                new WebAuthnAuthenticationProvider(relyingPartyOperations, userDetailsService));
    }
}
