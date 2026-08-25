package io.github.susimsek.springauthserversamples.service.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionTest {

    @Test
    void createsExceptionsForEachFactoryMethod() {
        Throwable cause = new IllegalStateException("boom");

        ApiException badRequest =
                ApiException.badRequest("field", "bad_request", "Invalid request");
        ApiException notFound = ApiException.notFound("Missing");
        ApiException conflict = ApiException.conflict("conflict", "Duplicate");
        ApiException forbidden = ApiException.forbidden("forbidden", "Denied");
        ApiException serverError = ApiException.serverError("server_error", "Broken", cause);

        assertThat(badRequest.getErrorCode()).isEqualTo(ApiErrorCode.INVALID_REQUEST);
        assertThat(badRequest.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badRequest.getErrorCodeValue()).isEqualTo("invalid_request");
        assertThat(badRequest.getField()).isEqualTo("field");
        assertThat(notFound.getErrorCode()).isEqualTo(ApiErrorCode.RESOURCE_NOT_FOUND);
        assertThat(conflict.getErrorCode()).isEqualTo(ApiErrorCode.CONFLICT);
        assertThat(forbidden.getErrorCode()).isEqualTo(ApiErrorCode.FORBIDDEN);
        assertThat(serverError.getErrorCode()).isEqualTo(ApiErrorCode.INTERNAL_ERROR);
        assertThat(serverError.getCause()).isSameAs(cause);
    }
}
