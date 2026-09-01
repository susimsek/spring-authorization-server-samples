package io.github.susimsek.springauthserversamples.config.security;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration(proxyBeanMethods = false)
public class AccountApiSecurityConfig {

    private static final String ACCOUNT_CONSOLE_CLIENT_ID = "account-console";

    @Bean
    @Order(2)
    SecurityFilterChain accountApiSecurityFilterChain(
            HttpSecurity http, JwtDecoder accountApiJwtDecoder) throws Exception {
        http.securityMatcher("/api/account/**")
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(
                        securityContext ->
                                securityContext.securityContextRepository(
                                        new NullSecurityContextRepository()))
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                                        .sessionFixation(sessionFixation -> sessionFixation.none()))
                .authorizeHttpRequests(
                        authorize -> authorize.anyRequest().hasAuthority("SCOPE_account-api"))
                .oauth2ResourceServer(
                        resourceServer ->
                                resourceServer.jwt(jwt -> jwt.decoder(accountApiJwtDecoder)));
        return http.build();
    }

    @Bean
    JwtDecoder accountApiJwtDecoder(
            JWKSource<SecurityContext> jwkSource,
            ApplicationProperties applicationProperties,
            AuthorizationRepository authorizationRepository) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSource(jwkSource).build();
        OAuth2TokenValidator<Jwt> audienceValidator =
                jwt ->
                        jwt.getAudience().contains(ACCOUNT_CONSOLE_CLIENT_ID)
                                ? OAuth2TokenValidatorResult.success()
                                : OAuth2TokenValidatorResult.failure(
                                        new OAuth2Error(
                                                "invalid_token", "Invalid token audience", null));
        jwtDecoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(
                                applicationProperties.authorizationServer().issuer()),
                        audienceValidator,
                        new ActiveAuthorizationTokenValidator(authorizationRepository)));
        return jwtDecoder;
    }
}
