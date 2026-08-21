package io.github.susimsek.springauthserversamples.web.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springauthserversamples.web.admin.validation.CreateValidation;
import io.github.susimsek.springauthserversamples.web.admin.validation.UpdateValidation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdminRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidPublicClientConfiguration() {
        AdminClientRequest request =
                new AdminClientRequest(
                        "client",
                        "Client",
                        Set.of("none"),
                        Set.of("authorization_code"),
                        Set.of(),
                        Set.of(),
                        Set.of("openid"),
                        false,
                        false,
                        null,
                        null,
                        null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("redirectUris", "authorizationGrantTypes");
    }

    @Test
    void validatesRequiredFieldsWhenCreatingAUser() {
        AdminUserRequest request = new AdminUserRequest("", "short", null, Set.of());

        assertThat(validator.validate(request, CreateValidation.class))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("username", "password", "enabled", "roles");
    }

    @Test
    void permitsAnEmptyPasswordWhenUpdatingAUser() {
        AdminUserRequest request = new AdminUserRequest("user", "", true, Set.of("ROLE_USER"));

        assertThat(validator.validate(request, UpdateValidation.class)).isEmpty();
    }
}
