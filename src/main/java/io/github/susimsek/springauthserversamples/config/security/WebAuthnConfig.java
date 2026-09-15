package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
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
        String origin = issuer.getScheme() + "://" + issuer.getRawAuthority();
        Webauthn4JRelyingPartyOperations operations =
                new Webauthn4JRelyingPartyOperations(
                        userEntities,
                        userCredentials,
                        PublicKeyCredentialRpEntity.builder()
                                .id(issuer.getHost())
                                .name("Spring Authorization Server Samples")
                                .build(),
                        Set.of(origin));
        operations.setCustomizeCreationOptions(
                options ->
                        options.timeout(Duration.ofMinutes(5))
                                .authenticatorSelection(
                                        org.springframework.security.web.webauthn.api
                                                .AuthenticatorSelectionCriteria.builder()
                                                .residentKey(
                                                        org.springframework.security.web.webauthn
                                                                .api.ResidentKeyRequirement
                                                                .REQUIRED)
                                                .userVerification(
                                                        UserVerificationRequirement.REQUIRED)
                                                .build()));
        operations.setCustomizeRequestOptions(
                options ->
                        options.timeout(Duration.ofMinutes(5))
                                .userVerification(UserVerificationRequirement.REQUIRED));
        return operations;
    }

    @Bean
    AuthenticationManager webAuthnAuthenticationManager(
            WebAuthnRelyingPartyOperations relyingPartyOperations,
            UserDetailsService userDetailsService) {
        return new ProviderManager(
                new WebAuthnAuthenticationProvider(relyingPartyOperations, userDetailsService));
    }
}
