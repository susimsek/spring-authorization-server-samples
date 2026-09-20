package io.github.susimsek.springauthserversamples.service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RegistrationCaptchaServiceTest {

    @Test
    void disabledCaptchaDoesNotRequireAConversationWithGoogle() {
        RegistrationCaptchaService service =
                service(
                        configuration(
                                false, "disabled", "", "", "", "", "register", false, 0.7, false));

        service.verifyOrThrow(null, org.mockito.Mockito.mock(HttpServletRequest.class));

        assertThat(service.publicSettings().enabled()).isFalse();
    }

    @Test
    void verifiesStandardV2ResponseAndSendsRemoteAddress() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        server.expect(requestTo("https://www.google.com/recaptcha/api/siteverify"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                "{\"success\":true}",
                                org.springframework.http.MediaType.APPLICATION_JSON));
        RegistrationCaptchaService service =
                new RegistrationCaptchaService(
                        new RegistrationCaptchaConfiguration(
                                true,
                                "recaptcha",
                                "site-key",
                                "secret",
                                "",
                                "",
                                "register",
                                false,
                                0.7,
                                false),
                        restClient);
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        org.mockito.Mockito.when(request.getRemoteAddr()).thenReturn("192.0.2.10");

        service.verifyOrThrow("token", request);

        server.verify();
    }

    @Test
    void rejectsAnInvalidEnterpriseActionOrScore() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        server.expect(
                        requestTo(
                                org.hamcrest.Matchers.containsString(
                                        "recaptchaenterprise.googleapis.com")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                "{\"tokenProperties\":{\"valid\":true,\"action\":\"login\"},"
                                        + "\"riskAnalysis\":{\"score\":0.99},"
                                        + "\"event\":{\"expectedAction\":\"register\"}}",
                                org.springframework.http.MediaType.APPLICATION_JSON));
        RegistrationCaptchaService service =
                new RegistrationCaptchaService(
                        new RegistrationCaptchaConfiguration(
                                true,
                                "enterprise",
                                "site-key",
                                "",
                                "project",
                                "api-key",
                                "register",
                                true,
                                0.7,
                                false),
                        restClient);

        assertThatThrownBy(
                        () ->
                                service.verifyOrThrow(
                                        "token",
                                        org.mockito.Mockito.mock(HttpServletRequest.class)))
                .hasFieldOrPropertyWithValue("errorCodeValue", "captcha_failed");
        server.verify();
    }

    @Test
    void hidesAnIncompleteConfigurationFromThePublicPageAndFailsClosed() {
        RegistrationCaptchaService service =
                service(
                        configuration(
                                true,
                                "enterprise",
                                "site-key",
                                "",
                                "project",
                                "",
                                "register",
                                true,
                                0.7,
                                false));

        assertThat(service.publicSettings().enabled()).isFalse();
        assertThatThrownBy(
                        () ->
                                service.verifyOrThrow(
                                        "token",
                                        org.mockito.Mockito.mock(HttpServletRequest.class)))
                .hasFieldOrPropertyWithValue("errorCodeValue", "captcha_failed");
    }

    private static RegistrationCaptchaService service(
            RegistrationCaptchaConfiguration configuration) {
        return new RegistrationCaptchaService(configuration, RestClient.builder().build());
    }

    private static RegistrationCaptchaConfiguration configuration(
            boolean enabled,
            String provider,
            String siteKey,
            String secretKey,
            String projectId,
            String apiKey,
            String action,
            boolean recaptchaV3,
            double scoreThreshold,
            boolean useRecaptchaNet) {
        return new RegistrationCaptchaConfiguration(
                enabled,
                provider,
                siteKey,
                secretKey,
                projectId,
                apiKey,
                action,
                recaptchaV3,
                scoreThreshold,
                useRecaptchaNet);
    }
}
