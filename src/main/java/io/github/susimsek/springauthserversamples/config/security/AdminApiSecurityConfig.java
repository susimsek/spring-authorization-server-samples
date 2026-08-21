package io.github.susimsek.springauthserversamples.config.security;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import java.util.ArrayList;
import java.util.Collection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class AdminApiSecurityConfig {

    private static final String ADMIN_CONSOLE_CLIENT_ID = "admin-console";

    @Bean
    @Order(1)
    SecurityFilterChain adminApiSecurityFilterChain(
            HttpSecurity http, JwtDecoder adminApiJwtDecoder) {
        http.securityMatcher("/api/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(HttpMethod.GET, "/api/admin/dashboard")
                                        .hasAuthority("ROLE_ADMIN")
                                        .requestMatchers("/api/admin/whoami")
                                        .hasAuthority("SCOPE_admin-api")
                                        .requestMatchers(HttpMethod.GET, "/api/admin/clients/**")
                                        .hasAnyAuthority(
                                                "ROLE_ADMIN",
                                                "ROLE_CLIENT_VIEWER",
                                                "ROLE_CLIENT_MANAGER")
                                        .requestMatchers("/api/admin/clients/**")
                                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_CLIENT_MANAGER")
                                        .requestMatchers(HttpMethod.GET, "/api/admin/users/**")
                                        .hasAnyAuthority(
                                                "ROLE_ADMIN",
                                                "ROLE_USER_VIEWER",
                                                "ROLE_USER_MANAGER")
                                        .requestMatchers(HttpMethod.PUT, "/api/admin/users/**")
                                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_USER_MANAGER")
                                        .requestMatchers(HttpMethod.POST, "/api/admin/users")
                                        .hasAuthority("ROLE_ADMIN")
                                        .requestMatchers(HttpMethod.DELETE, "/api/admin/users/**")
                                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_USER_MANAGER")
                                        .requestMatchers(HttpMethod.GET, "/api/admin/roles")
                                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_USER_MANAGER")
                                        .requestMatchers("/api/admin/sessions/**")
                                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_SESSION_MANAGER")
                                        .requestMatchers(HttpMethod.GET, "/api/admin/consents")
                                        .hasAnyAuthority(
                                                "ROLE_ADMIN",
                                                "ROLE_USER_VIEWER",
                                                "ROLE_USER_MANAGER")
                                        .requestMatchers(
                                                HttpMethod.DELETE, "/api/admin/consents/**")
                                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_USER_MANAGER")
                                        .requestMatchers("/api/admin/**")
                                        .hasAuthority("ROLE_ADMIN"))
                .oauth2ResourceServer(
                        resourceServer ->
                                resourceServer.jwt(
                                        jwt ->
                                                jwt.decoder(adminApiJwtDecoder)
                                                        .jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    JwtDecoder adminApiJwtDecoder(
            JWKSource<SecurityContext> jwkSource,
            ApplicationProperties applicationProperties,
            AuthorizationRepository authorizationRepository) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSource(jwkSource).build();
        OAuth2TokenValidator<Jwt> audienceValidator =
                jwt ->
                        jwt.getAudience().contains(ADMIN_CONSOLE_CLIENT_ID)
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

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeAuthorities = new JwtGrantedAuthoritiesConverter();
        JwtGrantedAuthoritiesConverter roleAuthorities = new JwtGrantedAuthoritiesConverter();
        roleAuthorities.setAuthoritiesClaimName("roles");
        roleAuthorities.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> mergeAuthorities(jwt, scopeAuthorities, roleAuthorities));
        return converter;
    }

    private static Collection<GrantedAuthority> mergeAuthorities(
            Jwt jwt,
            JwtGrantedAuthoritiesConverter scopeAuthorities,
            JwtGrantedAuthoritiesConverter roleAuthorities) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(scopeAuthorities.convert(jwt));
        authorities.addAll(roleAuthorities.convert(jwt));
        return authorities;
    }
}
