package io.github.susimsek.springauthserversamples.service.admin;

import org.springframework.http.HttpStatus;

public class AdminClientException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String field;

    private AdminClientException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, null, message);
    }

    private AdminClientException(
            HttpStatus status, String errorCode, String field, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.field = field;
    }

    private AdminClientException(
            HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.field = null;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getField() {
        return field;
    }

    public static AdminClientException badRequest(String errorCode, String message) {
        return new AdminClientException(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    public static AdminClientException badRequest(String field, String errorCode, String message) {
        return new AdminClientException(HttpStatus.BAD_REQUEST, errorCode, field, message);
    }

    public static AdminClientException notFound(String message) {
        return new AdminClientException(HttpStatus.NOT_FOUND, "admin_client_not_found", message);
    }

    public static AdminClientException conflict(String errorCode, String message) {
        return new AdminClientException(HttpStatus.CONFLICT, errorCode, message);
    }

    public static AdminClientException conflict(String field, String errorCode, String message) {
        return new AdminClientException(HttpStatus.CONFLICT, errorCode, field, message);
    }

    public static AdminClientException forbidden(String errorCode, String message) {
        return new AdminClientException(HttpStatus.FORBIDDEN, errorCode, message);
    }

    public static AdminClientException serverError(
            String errorCode, String message, Throwable cause) {
        return new AdminClientException(
                HttpStatus.INTERNAL_SERVER_ERROR, errorCode, message, cause);
    }
}
