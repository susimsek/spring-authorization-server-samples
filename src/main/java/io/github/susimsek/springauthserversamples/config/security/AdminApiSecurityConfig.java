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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class AdminApiSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain adminApiSecurityFilterChain(
            HttpSecurity http, JwtDecoder adminApiJwtDecoder) throws Exception {
        ConsoleApiSecurity.stateless(http);
        http.securityMatcher("/api/admin/**")
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(HttpMethod.GET, "/api/admin/dashboard")
                                        .hasAuthority("ROLE_ADMIN")
                                        .requestMatchers("/api/admin/whoami")
                                        .hasAuthority("SCOPE_admin-api")
                                        .requestMatchers(
                                                HttpMethod.GET, "/api/admin/client-scopes/**")
                                        .hasAnyAuthority(
                                                "ROLE_ADMIN",
                                                "ROLE_CLIENT_VIEWER",
                                                "ROLE_CLIENT_MANAGER")
                                        .requestMatchers("/api/admin/client-scopes/**")
                                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_CLIENT_MANAGER")
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
                                        .requestMatchers(HttpMethod.GET, "/api/admin/events")
                                        .hasAnyAuthority(
                                                "ROLE_ADMIN",
                                                "ROLE_USER_VIEWER",
                                                "ROLE_USER_MANAGER")
                                        .requestMatchers(HttpMethod.PUT, "/api/admin/users/**")
                                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_USER_MANAGER")
                                        .requestMatchers(HttpMethod.POST, "/api/admin/users")
                                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_USER_MANAGER")
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
        return ConsoleJwtDecoderFactory.create(
                jwkSource,
                applicationProperties.authorizationServer().issuer(),
                ConsoleClients.ADMIN,
                authorizationRepository);
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
