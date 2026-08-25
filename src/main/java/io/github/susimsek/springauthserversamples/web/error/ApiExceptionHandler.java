package io.github.susimsek.springauthserversamples.web.error;

import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import io.github.susimsek.springauthserversamples.web.ApiController;
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

/** Renders one RFC 9457 error contract for the Administration and Account APIs. */
@RestControllerAdvice(annotations = ApiController.class)
@Slf4j
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException exception, HttpServletRequest request) {
        return problemDetail(exception, request);
    }

    public ProblemDetail handleApiException(ApiException exception) {
        return problemDetail(exception, null);
    }

    @ExceptionHandler(Exception.class)
    public @Nullable ResponseEntity<Object> handleUnhandled(
            Exception exception, WebRequest request) {
        log.error("Unhandled API exception", exception);
        ProblemDetail problemDetail =
                createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR);
        requestUri(request).ifPresent(problemDetail::setInstance);
        return createResponseEntity(
                problemDetail, HttpHeaders.EMPTY, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<String> fields =
                exception.getBindingResult().getAllErrors().stream()
                        .map(ApiExceptionHandler::field)
                        .collect(
                                java.util.stream.Collectors.collectingAndThen(
                                        java.util.stream.Collectors.toCollection(
                                                LinkedHashSet::new),
                                        List::copyOf));
        return validationProblem(fields, headers, status, request);
    }

    @Override
    public ResponseEntity<Object> handleHandlerMethodValidationException(
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
                fields.isEmpty() ? List.of("request") : fields, headers, status, request);
    }

    @Override
    public ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return validationProblem(List.of("request"), headers, status, request);
    }

    private ProblemDetail problemDetail(ApiException exception, HttpServletRequest request) {
        ProblemDetail problemDetail =
                createProblemDetail(exception.getStatus(), exception.getErrorCode());
        if (request != null) {
            problemDetail.setInstance(URI.create(request.getRequestURI()));
        }
        if (exception.getField() != null) {
            problemDetail.setProperty("violations", List.of(Map.of("field", exception.getField())));
        }
        return problemDetail;
    }

    private ResponseEntity<Object> validationProblem(
            List<String> fields, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problemDetail =
                createProblemDetail(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
        requestUri(request).ifPresent(problemDetail::setInstance);
        problemDetail.setProperty(
                "violations", fields.stream().map(field -> Map.of("field", field)).toList());
        return createResponseEntity(problemDetail, headers, status, request);
    }

    private ProblemDetail createProblemDetail(HttpStatus status, ApiErrorCode errorCode) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        status, message(errorCode.messageCode(), errorCode.defaultMessage()));
        problemDetail.setTitle(message("app.api.problem.title", "API request failed"));
        problemDetail.setType(URI.create(errorCode.type()));
        problemDetail.setProperty("errorCode", errorCode.value());
        return problemDetail;
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

    private static java.util.Optional<URI> requestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return java.util.Optional.of(
                    URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
        return java.util.Optional.empty();
    }

    private String message(String code, String defaultMessage) {
        MessageSource messageSource = getMessageSource();
        return messageSource == null
                ? defaultMessage
                : messageSource.getMessage(
                        code, null, defaultMessage, LocaleContextHolder.getLocale());
    }
}
