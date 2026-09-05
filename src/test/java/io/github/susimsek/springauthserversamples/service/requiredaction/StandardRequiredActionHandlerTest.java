package io.github.susimsek.springauthserversamples.service.requiredaction;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StandardRequiredActionHandlerTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidProfileValues() {
        StandardRequiredActionHandler handler = new StandardRequiredActionHandler(validator);

        assertThatThrownBy(
                        () ->
                                handler.completeStandard(
                                        new UserEntity(),
                                        "UPDATE_PROFILE",
                                        Map.of(
                                                "firstName", "Ada",
                                                "lastName", "Lovelace",
                                                "email", "not-an-email")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsOversizedProfileValues() {
        StandardRequiredActionHandler handler = new StandardRequiredActionHandler(validator);

        assertThatThrownBy(
                        () ->
                                handler.completeStandard(
                                        new UserEntity(),
                                        "UPDATE_PROFILE",
                                        Map.of(
                                                "firstName", "A".repeat(101),
                                                "lastName", "Lovelace",
                                                "email", "ada@example.test")))
                .isInstanceOf(ApiException.class);
    }
}
