package io.github.susimsek.springauthserversamples.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.service.admin.AdminClientException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

class AdminClientExceptionHandlerTest {

    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void localizesProblemDetailFromTheRequestLocale() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage(
                "admin.error.title", Locale.forLanguageTag("tr"), "Yönetim isteği başarısız");
        messages.addMessage(
                "admin.error.admin_user_invalid_username",
                Locale.forLanguageTag("tr"),
                "Kullanıcı adı gerekli.");
        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));

        AdminClientExceptionHandler handler = handler(messages);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/users");
        var problem =
                handler.handleAdminClientException(
                        AdminClientException.badRequest(
                                "username", "admin_user_invalid_username", "Username is required"),
                        request);

        assertThat(problem.getTitle()).isEqualTo("Yönetim isteği başarısız");
        assertThat(problem.getDetail()).isEqualTo("Kullanıcı adı gerekli.");
        assertThat(problem.getType()).hasToString("urn:problem:admin_user_invalid_username");
        assertThat(problem.getInstance()).hasToString("/api/admin/users");
        assertThat(problem.getProperties())
                .containsEntry("errorCode", "admin_user_invalid_username")
                .containsEntry(
                        "violations", java.util.List.of(java.util.Map.of("field", "username")));
    }

    @Test
    void returnsProblemWithoutViolationsWhenFieldIsMissing() {
        var problem =
                handler(new StaticMessageSource())
                        .handleAdminClientException(
                                AdminClientException.forbidden(
                                        "admin_access_denied", "Access denied"));

        assertThat(problem.getStatus()).isEqualTo(403);
        assertThat(problem.getDetail()).isEqualTo("Access denied");
        assertThat(problem.getType()).hasToString("urn:problem:admin_access_denied");
        assertThat(problem.getProperties())
                .containsEntry("errorCode", "admin_access_denied")
                .doesNotContainKey("violations");
    }

    @Test
    void returnsLocalizedViolationsForBeanValidationFailures() throws NoSuchMethodException {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage(
                "admin.validation.title", Locale.forLanguageTag("tr"), "Doğrulama başarısız");
        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "adminClientRequest");
        bindingResult.addError(
                new FieldError(
                        "adminClientRequest",
                        "redirectUris[0]",
                        "Geçerli mutlak URI değerleri girin."));
        bindingResult.addError(
                new FieldError(
                        "adminClientRequest",
                        "redirectUris[1]",
                        "Geçerli mutlak URI değerleri girin."));
        bindingResult.addError(new ObjectError("adminClientRequest", "İstek geçersiz."));
        Method method =
                AdminClientExceptionHandlerTest.class.getDeclaredMethod("request", String.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/clients");

        var response =
                handler(messages)
                        .handleMethodArgumentNotValid(
                                exception,
                                HttpHeaders.EMPTY,
                                HttpStatus.BAD_REQUEST,
                                new ServletWebRequest(request));
        var problem = (org.springframework.http.ProblemDetail) response.getBody();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Doğrulama başarısız");
        assertThat(problem.getDetail()).isEqualTo("Geçerli mutlak URI değerleri girin.");
        assertThat(problem.getType()).hasToString("urn:problem:validation_failed");
        assertThat(problem.getInstance()).hasToString("/api/admin/clients");
        assertThat(problem.getProperties())
                .containsEntry("errorCode", "validation_failed")
                .containsEntry(
                        "violations",
                        java.util.List.of(
                                java.util.Map.of("field", "redirectUris"),
                                java.util.Map.of("field", "request")));
    }

    @Test
    void returnsRequestViolationForUnreadablePayload() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/clients");

        var response =
                handler(new StaticMessageSource())
                        .handleHttpMessageNotReadable(
                                new HttpMessageNotReadableException(
                                        "broken payload", new MockHttpInputMessage(new byte[0])),
                                HttpHeaders.EMPTY,
                                HttpStatus.BAD_REQUEST,
                                new ServletWebRequest(request));
        var problem = (org.springframework.http.ProblemDetail) response.getBody();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Validation failed");
        assertThat(problem.getDetail()).isEqualTo("The request contains an invalid value.");
        assertThat(problem.getInstance()).hasToString("/api/admin/clients");
        assertThat(problem.getProperties())
                .containsEntry("errorCode", "validation_failed")
                .containsEntry(
                        "violations", java.util.List.of(java.util.Map.of("field", "request")));
    }

    @Test
    void returnsParameterNamesForHandlerMethodValidation() throws NoSuchMethodException {
        Method method =
                AdminClientExceptionHandlerTest.class.getDeclaredMethod(
                        "validatedRequest", String.class, String.class);
        MethodParameter namedParameter = mock(MethodParameter.class);
        when(namedParameter.getParameterName()).thenReturn("clientId");
        MethodParameter unnamedParameter = mock(MethodParameter.class);
        when(unnamedParameter.getParameterName()).thenReturn(null);
        ParameterValidationResult namedResult = parameterResult(namedParameter);
        ParameterValidationResult unnamedResult = parameterResult(unnamedParameter);
        MethodValidationResult validationResult = mock(MethodValidationResult.class);
        when(validationResult.getTarget()).thenReturn(new Object());
        when(validationResult.getMethod()).thenReturn(method);
        when(validationResult.isForReturnValue()).thenReturn(false);
        when(validationResult.getCrossParameterValidationResults()).thenReturn(List.of());
        when(validationResult.getParameterValidationResults())
                .thenReturn(List.of(namedResult, unnamedResult));
        HandlerMethodValidationException exception =
                new HandlerMethodValidationException(validationResult);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/clients");

        var response =
                handler(new StaticMessageSource())
                        .handleHandlerMethodValidationException(
                                exception,
                                HttpHeaders.EMPTY,
                                HttpStatus.BAD_REQUEST,
                                new ServletWebRequest(request));
        var problem = (org.springframework.http.ProblemDetail) response.getBody();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Validation failed");
        assertThat(problem.getProperties())
                .containsEntry("errorCode", "validation_failed")
                .containsEntry(
                        "violations",
                        java.util.List.of(
                                java.util.Map.of("field", "clientId"),
                                java.util.Map.of("field", "request")));
    }

    @Test
    void doesNotExposeUnhandledExceptionDetails() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage(
                "admin.error.internal", Locale.ENGLISH, "An unexpected error occurred.");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/clients");

        var response =
                handler(messages)
                        .handleUnhandled(
                                new IllegalStateException("database connection details"),
                                new ServletWebRequest(request));
        var problem = (org.springframework.http.ProblemDetail) response.getBody();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getTitle()).isEqualTo("Admin request failed");
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred.");
        assertThat(problem.getDetail()).doesNotContain("database connection details");
        assertThat(problem.getType()).hasToString("urn:problem:internal_error");
        assertThat(problem.getInstance()).hasToString("/api/admin/clients");
        assertThat(problem.getProperties()).containsEntry("errorCode", "internal_error");
    }

    @SuppressWarnings("unused")
    private void request(String value) {}

    @SuppressWarnings("unused")
    private void validatedRequest(String clientId, String ignored) {}

    private static ParameterValidationResult parameterResult(MethodParameter parameter) {
        return new ParameterValidationResult(
                parameter,
                null,
                List.of(new DefaultMessageSourceResolvable("invalid")),
                null,
                null,
                null,
                (error, sourceType) -> null);
    }

    private static AdminClientExceptionHandler handler(StaticMessageSource messages) {
        AdminClientExceptionHandler handler = new AdminClientExceptionHandler();
        handler.setMessageSource(messages);
        return handler;
    }
}
