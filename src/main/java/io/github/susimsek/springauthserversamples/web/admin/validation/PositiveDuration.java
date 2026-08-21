package io.github.susimsek.springauthserversamples.web.admin.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PositiveDurationValidator.class)
@Retention(RUNTIME)
@Target({FIELD, PARAMETER})
public @interface PositiveDuration {

    String message() default "{admin.validation.ttl}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
