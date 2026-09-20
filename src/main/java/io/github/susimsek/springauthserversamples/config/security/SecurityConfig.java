package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.security.LocalizedAccessDeniedHandler;
import io.github.susimsek.springauthserversamples.security.LocalizedAuthenticationEntryPoint;
import io.github.susimsek.springauthserversamples.service.SocialLoginService;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsFilter;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private static final SecureRandom SOCIAL_STATE_RANDOM = new SecureRandom();

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
            LoginRateLimitFilter loginRateLimitFilter,
            LoginCaptchaFilter loginCaptchaFilter,
            ApplicationProperties applicationProperties,
            AuthenticationManager webAuthnAuthenticationManager,
            PublicKeyCredentialRequestOptionsRepository webAuthnRequestOptionsRepository,
            WebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            ObjectProvider<SocialLoginAuthenticationSuccessHandler> socialLoginSuccessHandler,
            ObjectProvider<OAuth2AuthorizationRequestResolver> socialAuthorizationRequestResolver,
            SocialLoginService socialLoginService,
            OAuth2AuthorizedClientRepository socialAuthorizedClientRepository,
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
                    socialTokenResponseClient) {
        URI issuer = URI.create(applicationProperties.authorizationServer().issuer());
        ApplicationProperties.WebAuthn policy = applicationProperties.webAuthn();
        String configuredRpId = policy.rpId() == null ? "" : policy.rpId().trim();
        String rpId = configuredRpId.isBlank() ? issuer.getHost() : configuredRpId;
        String configuredOrigins = policy.allowedOrigins() == null ? "" : policy.allowedOrigins();
        Set<String> allowedOrigins =
                configuredOrigins.isBlank()
                        ? Set.of(issuer.getScheme() + "://" + issuer.getRawAuthority())
                        : Arrays.stream(configuredOrigins.split(","))
                                .map(String::trim)
                                .filter(value -> !value.isBlank())
                                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
                new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/admin");
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
                                        .requestMatchers("/api/auth/mfa/**")
                                        .authenticated()
                                        .requestMatchers("/account/avatar")
                                        .authenticated()
                                        .requestMatchers("/account/social-links/**")
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
                                                "/mfa",
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
                                        .successHandler(
                                                new SocialAccountLinkingAuthenticationSuccessHandler(
                                                        socialLoginService, successHandler))
                                        .securityContextRepository(securityContextRepository)
                                        .permitAll())
                .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices));

        http.webAuthn(
                webAuthn ->
                        webAuthn.rpId(rpId)
                                .allowedOrigins(allowedOrigins)
                                .disableDefaultRegistrationPage(true));

        PublicKeyCredentialRequestOptionsFilter requestOptionsFilter =
                new PublicKeyCredentialRequestOptionsFilter(webAuthnRelyingPartyOperations);
        requestOptionsFilter.setRequestOptionsRepository(webAuthnRequestOptionsRepository);
        WebAuthnAuthenticationFilter authenticationFilter = new WebAuthnAuthenticationFilter();
        authenticationFilter.setAuthenticationManager(webAuthnAuthenticationManager);
        authenticationFilter.setRequestOptionsRepository(webAuthnRequestOptionsRepository);
        authenticationFilter.setAuthenticationSuccessHandler(
                (request, response, authentication) -> {
                    MfaAuthorizationFilter.markCredentialVerified(request.getSession(true));
                    if (request.getSession(false) != null
                            && request.getSession(false)
                                            .getAttribute(
                                                    MfaAuthorizationFilter.MFA_PENDING_REQUEST)
                                    != null) {
                        MfaAuthorizationFilter.markVerified(request.getSession(false));
                    }
                    new WebAuthnAuthenticationSuccessHandler()
                            .onAuthenticationSuccess(request, response, authentication);
                });
        authenticationFilter.setAuthenticationFailureHandler(
                new SimpleUrlAuthenticationFailureHandler("/login?error"));

        http.addFilterBefore(requestOptionsFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http.addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginCaptchaFilter, UsernamePasswordAuthenticationFilter.class);

        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(
                    oauth2 ->
                            oauth2.loginPage("/login")
                                    .authorizedClientRepository(socialAuthorizedClientRepository)
                                    .successHandler(socialLoginSuccessHandler.getObject())
                                    .authorizationEndpoint(
                                            authorizationEndpoint ->
                                                    authorizationEndpoint
                                                            .authorizationRequestResolver(
                                                                    socialAuthorizationRequestResolver
                                                                            .getObject()))
                                    .tokenEndpoint(
                                            tokenEndpoint ->
                                                    tokenEndpoint.accessTokenResponseClient(
                                                            socialTokenResponseClient))
                                    .failureHandler(
                                            new SimpleUrlAuthenticationFailureHandler(
                                                    "/login?error"))
                                    .permitAll());
        }

        http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    OAuth2AuthorizedClientRepository socialAuthorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
            socialTokenResponseClient() {
        RestClientAuthorizationCodeTokenResponseClient client =
                new RestClientAuthorizationCodeTokenResponseClient();
        return client;
    }

    @Bean
    OAuth2AuthorizationRequestResolver socialAuthorizationRequestResolver(
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
            SocialLoginService socialLoginService) {
        ClientRegistrationRepository clientRegistrationRepository =
                clientRegistrationRepositoryProvider.getIfAvailable();
        if (clientRegistrationRepository == null) {
            return new OAuth2AuthorizationRequestResolver() {
                @Override
                public OAuth2AuthorizationRequest resolve(
                        jakarta.servlet.http.HttpServletRequest request) {
                    return null;
                }

                @Override
                public OAuth2AuthorizationRequest resolve(
                        jakarta.servlet.http.HttpServletRequest request,
                        String clientRegistrationId) {
                    return null;
                }
            };
        }
        DefaultOAuth2AuthorizationRequestResolver delegate =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        DefaultOAuth2AuthorizationRequestResolver
                                .DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(
                    jakarta.servlet.http.HttpServletRequest request) {
                return withoutLinkedInNonce(
                        withShortStateParameter(
                                onlyEnabled(delegate.resolve(request), socialLoginService),
                                socialLoginService),
                        socialLoginService);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(
                    jakarta.servlet.http.HttpServletRequest request, String clientRegistrationId) {
                if (!socialLoginService.isProviderLoginAllowed(clientRegistrationId)) {
                    return null;
                }
                return withoutLinkedInNonce(
                        withShortStateParameter(
                                delegate.resolve(request, clientRegistrationId),
                                socialLoginService),
                        socialLoginService);
            }
        };
    }

    private static OAuth2AuthorizationRequest onlyEnabled(
            OAuth2AuthorizationRequest authorizationRequest,
            SocialLoginService socialLoginService) {
        if (authorizationRequest == null) {
            return null;
        }
        String registrationId =
                authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);
        return socialLoginService.isProviderLoginAllowed(registrationId)
                ? authorizationRequest
                : null;
    }

    private static OAuth2AuthorizationRequest withoutLinkedInNonce(
            OAuth2AuthorizationRequest authorizationRequest,
            SocialLoginService socialLoginService) {
        if (authorizationRequest == null
                || !socialLoginService.isLinkedInProvider(
                        authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID))) {
            return authorizationRequest;
        }
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(parameters -> parameters.remove(OidcParameterNames.NONCE))
                .attributes(attributes -> attributes.remove(OidcParameterNames.NONCE))
                .build();
    }

    private static OAuth2AuthorizationRequest withShortStateParameter(
            OAuth2AuthorizationRequest authorizationRequest,
            SocialLoginService socialLoginService) {
        if (authorizationRequest == null
                || !socialLoginService.requiresShortStateParameter(
                        authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID))) {
            return authorizationRequest;
        }
        return OAuth2AuthorizationRequest.from(authorizationRequest).state(shortState()).build();
    }

    static String shortState() {
        byte[] bytes = new byte[16];
        SOCIAL_STATE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Bean
    SecurityContextRepository browserSecurityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    @Bean
    SocialLoginAuthenticationSuccessHandler socialLoginAuthenticationSuccessHandler(
            io.github.susimsek.springauthserversamples.service.SocialLoginService
                    socialLoginService,
            org.springframework.security.core.userdetails.UserDetailsService userDetailsService,
            @Qualifier("browserSecurityContextRepository")
                    SecurityContextRepository securityContextRepository,
            OAuth2AuthorizedClientRepository socialAuthorizedClientRepository,
            io.github.susimsek.springauthserversamples.service.SocialTokenService
                    socialTokenService,
            io.github.susimsek.springauthserversamples.service.account.MfaService mfaService) {
        return new SocialLoginAuthenticationSuccessHandler(
                socialLoginService,
                userDetailsService,
                securityContextRepository,
                socialAuthorizedClientRepository,
                socialTokenService,
                mfaService);
    }

    @Bean
    SecurityContextRepository authorizationServerSecurityContextRepository() {
        return new AuthorizationServerSecurityContextRepository();
    }

    @Bean
    SocialProviderLogoutSuccessHandler socialProviderLogoutSuccessHandler(
            io.github.susimsek.springauthserversamples.service.SocialProviderSettingsService
                    providerSettingsService,
            SocialProviderLogoutEndpointResolver logoutEndpointResolver) {
        return new SocialProviderLogoutSuccessHandler(
                providerSettingsService, logoutEndpointResolver);
    }

    @Bean
    AuthenticationEventPublisher authenticationEventPublisher(
            ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    }
}
