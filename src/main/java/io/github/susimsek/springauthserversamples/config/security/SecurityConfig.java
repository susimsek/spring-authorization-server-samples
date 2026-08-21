package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.security.LocalizedAccessDeniedHandler;
import io.github.susimsek.springauthserversamples.security.LocalizedAuthenticationEntryPoint;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final MediaTypeRequestMatcher HTML_REQUEST_MATCHER = htmlRequestMatcher();

    private static final NegatedRequestMatcher NON_HTML_REQUEST_MATCHER =
            new NegatedRequestMatcher(HTML_REQUEST_MATCHER);

    private final LocalizedAuthenticationEntryPoint localizedAuthenticationEntryPoint;
    private final LocalizedAccessDeniedHandler localizedAccessDeniedHandler;

    private static MediaTypeRequestMatcher htmlRequestMatcher() {
        MediaTypeRequestMatcher requestMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        requestMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return requestMatcher;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @Order(2)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers("/avatars/**")
                                        .permitAll()
                                        .requestMatchers("/account/avatar")
                                        .authenticated()
                                        .requestMatchers("/api/account/avatar")
                                        .hasAuthority("SCOPE_profile")
                                        .requestMatchers(
                                                "/admin",
                                                "/admin/**",
                                                "/en/admin/**",
                                                "/tr/admin/**",
                                                "/",
                                                "/login",
                                                "/login/**",
                                                "/en/**",
                                                "/tr/**",
                                                "/_next/**",
                                                "/actuator/health",
                                                "/actuator/health/**",
                                                "/error")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .defaultAuthenticationEntryPointFor(
                                                new LoginUrlAuthenticationEntryPoint("/login"),
                                                HTML_REQUEST_MATCHER)
                                        .defaultAuthenticationEntryPointFor(
                                                localizedAuthenticationEntryPoint,
                                                NON_HTML_REQUEST_MATCHER)
                                        .accessDeniedHandler(localizedAccessDeniedHandler))
                .formLogin(formLogin -> formLogin.loginPage("/login").permitAll());

        http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
