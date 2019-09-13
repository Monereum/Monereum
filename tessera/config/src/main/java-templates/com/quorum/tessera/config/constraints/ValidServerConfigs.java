package com.quorum.tessera.config.constraints;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE,TYPE_PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = ServerConfigsValidator.class)
@Documented
public @interface ValidServerConfigs {

    String message() default "{ValidServerConfigs.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
