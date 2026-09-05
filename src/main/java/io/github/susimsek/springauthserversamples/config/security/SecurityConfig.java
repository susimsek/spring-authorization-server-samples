package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.security.LocalizedAccessDeniedHandler;
import io.github.susimsek.springauthserversamples.security.LocalizedAuthenticationEntryPoint;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private static final MediaTypeRequestMatcher HTML_REQUEST_MATCHER = htmlRequestMatcher();

    private static final NegatedRequestMatcher NON_HTML_REQUEST_MATCHER =
            new NegatedRequestMatcher(HTML_REQUEST_MATCHER);

    private final LocalizedAuthenticationEntryPoint localizedAuthenticationEntryPoint;
    private final LocalizedAccessDeniedHandler localizedAccessDeniedHandler;
    private final DynamicRememberMeServices rememberMeServices;

    @org.springframework.beans.factory.annotation.Autowired
    public SecurityConfig(
            LocalizedAuthenticationEntryPoint localizedAuthenticationEntryPoint,
            LocalizedAccessDeniedHandler localizedAccessDeniedHandler,
            DynamicRememberMeServices rememberMeServices) {
        this.localizedAuthenticationEntryPoint = localizedAuthenticationEntryPoint;
        this.localizedAccessDeniedHandler = localizedAccessDeniedHandler;
        this.rememberMeServices = rememberMeServices;
    }

    public SecurityConfig(
            LocalizedAuthenticationEntryPoint localizedAuthenticationEntryPoint,
            LocalizedAccessDeniedHandler localizedAccessDeniedHandler) {
        this(localizedAuthenticationEntryPoint, localizedAccessDeniedHandler, null);
    }

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
    @Order(3)
    SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            @Qualifier("browserSecurityContextRepository")
                    SecurityContextRepository securityContextRepository,
            LoginRateLimitFilter loginRateLimitFilter) {
        http.securityContext(
                        securityContext ->
                                securityContext
                                        .securityContextRepository(securityContextRepository)
                                        .requireExplicitSave(false))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        sessionManagement ->
                                sessionManagement
                                        .requireExplicitAuthenticationStrategy(true)
                                        .sessionFixation(
                                                sessionFixation ->
                                                        sessionFixation.changeSessionId()))
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers("/avatars/**", "/oidc/session-iframe.html")
                                        .permitAll()
                                        .requestMatchers("/account/avatar")
                                        .authenticated()
                                        .requestMatchers("/api/auth/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/admin",
                                                "/admin/**",
                                                "/",
                                                "/index.html",
                                                "/404.html",
                                                "/account/**",
                                                "/account",
                                                "/consent",
                                                "/auth-error",
                                                "/forgot-password",
                                                "/reset-password",
                                                "/verify-email",
                                                "/confirm-email",
                                                "/required-actions",
                                                "/login",
                                                "/login/**",
                                                "/register",
                                                "/_next/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui.html",
                                                "/swagger-ui/**",
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
                .formLogin(
                        formLogin ->
                                formLogin
                                        .loginPage("/login")
                                        .securityContextRepository(securityContextRepository)
                                        .permitAll())
                .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices));

        http.addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    SecurityContextRepository browserSecurityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    @Bean
    SecurityContextRepository authorizationServerSecurityContextRepository() {
        return new AuthorizationServerSecurityContextRepository();
    }

    @Bean
    AuthenticationEventPublisher authenticationEventPublisher(
            ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    }
}
