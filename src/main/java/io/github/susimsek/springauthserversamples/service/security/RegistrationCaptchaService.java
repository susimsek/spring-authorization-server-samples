package io.github.susimsek.springauthserversamples.service.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.susimsek.springauthserversamples.dto.account.RegistrationCaptchaDTO;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Verifies registration CAPTCHA tokens using the Google APIs used by Keycloak. */
@Service
@Slf4j
public class RegistrationCaptchaService {

    private static final String DEFAULT_ACTION = "register";

    private final RegistrationCaptchaSettingsService settingsService;
    private final RegistrationCaptchaConfiguration fixedConfiguration;
    private final RestClient restClient;

    @Autowired
    public RegistrationCaptchaService(RegistrationCaptchaSettingsService settingsService) {
        this(settingsService, null, RestClient.builder().build());
    }

    RegistrationCaptchaService(
            RegistrationCaptchaConfiguration configuration, RestClient restClient) {
        this(null, configuration, restClient);
    }

    private RegistrationCaptchaService(
            RegistrationCaptchaSettingsService settingsService,
            RegistrationCaptchaConfiguration fixedConfiguration,
            RestClient restClient) {
        this.settingsService = settingsService;
        this.fixedConfiguration = fixedConfiguration;
        this.restClient = restClient;
    }

    public RegistrationCaptchaDTO publicSettings() {
        return publicSettings(publicConfiguration());
    }

    public RegistrationCaptchaDTO publicLoginSettings() {
        return publicSettings(loginPublicConfiguration());
    }

    private RegistrationCaptchaDTO publicSettings(RegistrationCaptchaConfiguration config) {
        if (!isEnabled(config) || !isConfigured(config)) {
            return new RegistrationCaptchaDTO(false, "", "", "", false, false);
        }
        return new RegistrationCaptchaDTO(
                true,
                provider(config),
                config.siteKey().trim(),
                action(config),
                config.recaptchaV3(),
                config.useRecaptchaNet());
    }

    public void verifyOrThrow(String token, HttpServletRequest request) {
        verifyOrThrow(token, request, verificationConfiguration());
    }

    public void verifyLoginOrThrow(String token, HttpServletRequest request) {
        verifyOrThrow(token, request, loginVerificationConfiguration());
    }

    private void verifyOrThrow(
            String token, HttpServletRequest request, RegistrationCaptchaConfiguration config) {
        if (!isEnabled(config)) {
            return;
        }
        if (!isConfigured(config)
                || token == null
                || token.isBlank()
                || !verify(config, token.trim(), request)) {
            throw ApiException.badRequest(
                    "captchaToken", ApiErrorCode.CAPTCHA_FAILED, "CAPTCHA verification failed");
        }
    }

    private boolean verify(
            RegistrationCaptchaConfiguration config, String token, HttpServletRequest request) {
        try {
            return "enterprise".equals(provider(config))
                    ? verifyEnterprise(config, token, request)
                    : verifyStandard(config, token, request);
        } catch (Exception exception) {
            log.warn("Registration CAPTCHA verification failed", exception);
            return false;
        }
    }

    private boolean verifyStandard(
            RegistrationCaptchaConfiguration config, String token, HttpServletRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", config.secretKey().trim());
        form.add("response", token);
        if (request.getRemoteAddr() != null && !request.getRemoteAddr().isBlank()) {
            form.add("remoteip", request.getRemoteAddr());
        }
        StandardResponse response =
                restClient
                        .post()
                        .uri("https://www." + domain(config) + "/recaptcha/api/siteverify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(StandardResponse.class);
        if (response == null || !response.success()) {
            return false;
        }
        if (!config.recaptchaV3()) {
            return true;
        }
        return actionMatches(response.action(), config)
                && response.score() != null
                && response.score() >= config.scoreThreshold();
    }

    private boolean verifyEnterprise(
            RegistrationCaptchaConfiguration config, String token, HttpServletRequest request) {
        EnterpriseEvent event =
                new EnterpriseEvent(
                        token,
                        config.siteKey().trim(),
                        request.getHeader("User-Agent"),
                        request.getRemoteAddr(),
                        action(config));
        EnterpriseResponse response =
                restClient
                        .post()
                        .uri(
                                uriBuilder ->
                                        uriBuilder
                                                .scheme("https")
                                                .host("recaptchaenterprise.googleapis.com")
                                                .path("/v1/projects/{projectId}/assessments")
                                                .queryParam("key", config.apiKey().trim())
                                                .build(
                                                        Map.of(
                                                                "projectId",
                                                                config.projectId().trim())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new EnterpriseRequest(event))
                        .retrieve()
                        .body(EnterpriseResponse.class);
        if (response == null
                || response.tokenProperties() == null
                || response.riskAnalysis() == null
                || response.event() == null) {
            return false;
        }
        return response.tokenProperties().valid()
                && actionMatches(
                        response.tokenProperties().action(), response.event().expectedAction())
                && response.riskAnalysis().score() >= config.scoreThreshold();
    }

    private static boolean isConfigured(RegistrationCaptchaConfiguration config) {
        if ("recaptcha".equals(provider(config))) {
            return present(config.siteKey())
                    && present(config.secretKey())
                    && (!config.recaptchaV3()
                            || (validAction(action(config))
                                    && validScoreThreshold(config.scoreThreshold())));
        }
        return "enterprise".equals(provider(config))
                && present(config.siteKey())
                && present(config.projectId())
                && present(config.apiKey())
                && validAction(action(config))
                && validScoreThreshold(config.scoreThreshold());
    }

    private static boolean isEnabled(RegistrationCaptchaConfiguration config) {
        return config.enabled()
                && ("recaptcha".equals(provider(config)) || "enterprise".equals(provider(config)));
    }

    private static boolean actionMatches(String actual, RegistrationCaptchaConfiguration config) {
        return actionMatches(actual, action(config));
    }

    private static boolean actionMatches(String actual, String expected) {
        return actual != null && actual.equals(expected);
    }

    private static boolean validScoreThreshold(double scoreThreshold) {
        return scoreThreshold >= 0.0 && scoreThreshold <= 1.0;
    }

    private static boolean validAction(String action) {
        return action != null && action.matches("[A-Za-z0-9/_]+");
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static String provider(RegistrationCaptchaConfiguration config) {
        return config.provider() == null
                ? "disabled"
                : config.provider().trim().toLowerCase(Locale.ROOT);
    }

    private static String action(RegistrationCaptchaConfiguration config) {
        return present(config.action()) ? config.action().trim() : DEFAULT_ACTION;
    }

    private static String domain(RegistrationCaptchaConfiguration config) {
        return config.useRecaptchaNet() ? "recaptcha.net" : "google.com";
    }

    private RegistrationCaptchaConfiguration publicConfiguration() {
        return settingsService == null ? fixedConfiguration : settingsService.publicConfiguration();
    }

    private RegistrationCaptchaConfiguration verificationConfiguration() {
        return settingsService == null
                ? fixedConfiguration
                : settingsService.verificationConfiguration();
    }

    private RegistrationCaptchaConfiguration loginPublicConfiguration() {
        return settingsService == null
                ? fixedConfiguration
                : settingsService.loginPublicConfiguration();
    }

    private RegistrationCaptchaConfiguration loginVerificationConfiguration() {
        return settingsService == null
                ? fixedConfiguration
                : settingsService.loginVerificationConfiguration();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StandardResponse(boolean success, Double score, String action) {}

    private record EnterpriseRequest(EnterpriseEvent event) {}

    private record EnterpriseEvent(
            String token,
            String siteKey,
            String userAgent,
            String userIpAddress,
            String expectedAction) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EnterpriseResponse(
            TokenProperties tokenProperties, RiskAnalysis riskAnalysis, EnterpriseEvent event) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenProperties(boolean valid, String action) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RiskAnalysis(double score) {}
}
