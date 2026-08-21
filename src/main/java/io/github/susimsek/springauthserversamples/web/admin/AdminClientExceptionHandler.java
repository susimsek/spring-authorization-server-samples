package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.service.admin.AdminClientException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(annotations = AdminApi.class)
@Slf4j
public class AdminClientExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AdminClientException.class)
    ProblemDetail handleAdminClientException(
            AdminClientException exception, HttpServletRequest request) {
        return problemDetail(exception, request);
    }

    ProblemDetail handleAdminClientException(AdminClientException exception) {
        return problemDetail(exception, null);
    }

    @ExceptionHandler(Exception.class)
    public @Nullable ResponseEntity<Object> handleUnhandled(
            Exception exception, WebRequest request) {
        log.error("Unhandled admin API exception", exception);
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        message("admin.error.internal", "An unexpected error occurred."));
        problemDetail.setTitle(message("admin.error.title", "Admin request failed"));
        problemDetail.setType(URI.create("urn:problem:internal_error"));
        requestUri(request).ifPresent(problemDetail::setInstance);
        problemDetail.setProperty("errorCode", "internal_error");
        return createResponseEntity(
                problemDetail, HttpHeaders.EMPTY, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<String> fields =
                exception.getBindingResult().getAllErrors().stream()
                        .map(AdminClientExceptionHandler::field)
                        .collect(
                                java.util.stream.Collectors.collectingAndThen(
                                        java.util.stream.Collectors.toCollection(
                                                LinkedHashSet::new),
                                        List::copyOf));
        String detail =
                exception.getBindingResult().getAllErrors().stream()
                        .findFirst()
                        .map(error -> error.getDefaultMessage())
                        .orElseGet(() -> message("admin.validation.required", "Validation failed"));
        return validationProblem(detail, fields, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<String> fields =
                exception.getParameterValidationResults().stream()
                        .map(ParameterValidationResult::getMethodParameter)
                        .map(parameter -> parameter.getParameterName())
                        .map(field -> field == null ? "request" : field)
                        .distinct()
                        .toList();
        return validationProblem(
                message("admin.validation.required", "Validation failed"),
                fields.isEmpty() ? List.of("request") : fields,
                headers,
                status,
                request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return validationProblem(
                message("admin.validation.request", "The request contains an invalid value."),
                List.of("request"),
                headers,
                status,
                request);
    }

    private ProblemDetail problemDetail(
            AdminClientException exception, HttpServletRequest request) {
        String messageCode = "admin.error." + exception.getErrorCode();
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        exception.getStatus(), message(messageCode, exception.getMessage()));
        problemDetail.setTitle(message("admin.error.title", "Admin request failed"));
        problemDetail.setType(URI.create("urn:problem:" + exception.getErrorCode()));
        if (request != null) {
            problemDetail.setInstance(URI.create(request.getRequestURI()));
        }
        problemDetail.setProperty("errorCode", exception.getErrorCode());
        java.util.Optional.ofNullable(exception.getField())
                .ifPresent(
                        field -> {
                            problemDetail.setProperty(
                                    "violations", List.of(Map.of("field", field)));
                        });
        return problemDetail;
    }

    private ResponseEntity<Object> validationProblem(
            String detail,
            List<String> fields,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(message("admin.validation.title", "Validation failed"));
        problemDetail.setType(URI.create("urn:problem:validation_failed"));
        requestUri(request).ifPresent(problemDetail::setInstance);
        problemDetail.setProperty("errorCode", "validation_failed");
        problemDetail.setProperty(
                "violations", fields.stream().map(field -> Map.of("field", field)).toList());
        return createResponseEntity(problemDetail, headers, status, request);
    }

    private static String field(ObjectError error) {
        if (!(error instanceof FieldError fieldError)) {
            return "request";
        }
        String field = fieldError.getField();
        int indexedField = field.indexOf('[');
        int nestedField = field.indexOf('.');
        int end = field.length();
        if (indexedField >= 0) {
            end = indexedField;
        }
        if (nestedField >= 0) {
            end = Math.min(end, nestedField);
        }
        return field.substring(0, end);
    }

    private String message(String code, String defaultMessage) {
        MessageSource messageSource = getMessageSource();
        return messageSource == null
                ? defaultMessage
                : messageSource.getMessage(
                        code, null, defaultMessage, LocaleContextHolder.getLocale());
    }

    private static java.util.Optional<URI> requestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return java.util.Optional.of(
                    URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
        return java.util.Optional.empty();
    }
}
