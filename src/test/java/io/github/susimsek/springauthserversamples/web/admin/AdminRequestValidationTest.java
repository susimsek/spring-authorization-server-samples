package io.github.susimsek.springauthserversamples.web.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springauthserversamples.dto.admin.AdminClientRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminUserRequestDTO;
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
        AdminClientRequestDTO request =
                new AdminClientRequestDTO(
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
        AdminUserRequestDTO request = new AdminUserRequestDTO("", "short", null, Set.of());

        assertThat(validator.validate(request, CreateValidation.class))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("username", "password", "enabled", "roles");
    }

    @Test
    void permitsAnEmptyPasswordWhenUpdatingAUser() {
        AdminUserRequestDTO request =
                new AdminUserRequestDTO("user", "", true, Set.of("ROLE_USER"));

        assertThat(validator.validate(request, UpdateValidation.class)).isEmpty();
    }
}
