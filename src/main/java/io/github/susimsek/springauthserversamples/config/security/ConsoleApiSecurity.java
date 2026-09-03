package io.github.susimsek.springauthserversamples.config.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.context.NullSecurityContextRepository;

final class ConsoleApiSecurity {

    private ConsoleApiSecurity() {}

    static void stateless(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .securityContext(
                        securityContext ->
                                securityContext.securityContextRepository(
                                        new NullSecurityContextRepository()))
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                                        .sessionFixation(
                                                SessionManagementConfigurer
                                                                .SessionFixationConfigurer
                                                        ::none));
    }
}
