package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AdminClientExceptionTest {

    @Test
    void createsExceptionsForEachFactoryMethod() {
        Throwable cause = new IllegalStateException("boom");

        AdminClientException badRequest =
                AdminClientException.badRequest("field", "bad_request", "Invalid request");
        AdminClientException notFound = AdminClientException.notFound("Missing");
        AdminClientException conflict = AdminClientException.conflict("conflict", "Duplicate");
        AdminClientException forbidden = AdminClientException.forbidden("forbidden", "Denied");
        AdminClientException serverError =
                AdminClientException.serverError("server_error", "Broken", cause);

        assertThat(badRequest.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badRequest.getErrorCode()).isEqualTo("bad_request");
        assertThat(badRequest.getField()).isEqualTo("field");
        assertThat(notFound.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(conflict.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(forbidden.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(serverError.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(serverError.getCause()).isSameAs(cause);
    }
}
