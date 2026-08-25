package io.github.susimsek.springauthserversamples.service.error;

import org.springframework.http.HttpStatus;

/** A stable, transport-neutral API failure rendered as an RFC 9457 problem detail. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ApiErrorCode errorCode;
    private final String field;

    private ApiException(HttpStatus status, ApiErrorCode errorCode, String field, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.field = field;
    }

    private ApiException(
            HttpStatus status, ApiErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.field = null;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCodeValue() {
        return errorCode.value();
    }

    public String getField() {
        return field;
    }

    public static ApiException badRequest(String ignoredErrorCode, String message) {
        return badRequest(null, ignoredErrorCode, message);
    }

    public static ApiException badRequest(String field, String ignoredErrorCode, String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.fromLegacyCode(ignoredErrorCode, ApiErrorCode.INVALID_REQUEST),
                field,
                message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, null, message);
    }

    public static ApiException conflict(String ignoredErrorCode, String message) {
        return conflict(null, ignoredErrorCode, message);
    }

    public static ApiException conflict(String field, String ignoredErrorCode, String message) {
        return new ApiException(
                HttpStatus.CONFLICT,
                ApiErrorCode.fromLegacyCode(ignoredErrorCode, ApiErrorCode.CONFLICT),
                field,
                message);
    }

    public static ApiException forbidden(String ignoredErrorCode, String message) {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                ApiErrorCode.fromLegacyCode(ignoredErrorCode, ApiErrorCode.FORBIDDEN),
                null,
                message);
    }

    public static ApiException serverError(
            String ignoredErrorCode, String message, Throwable cause) {
        return new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.fromLegacyCode(ignoredErrorCode, ApiErrorCode.INTERNAL_ERROR),
                message,
                cause);
    }
}
