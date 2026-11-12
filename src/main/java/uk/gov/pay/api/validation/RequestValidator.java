package uk.gov.pay.api.validation;

import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates what a resource method receives (its entity and its constrained headers) together, raising every
 * violation at once.
 */
@Singleton
public class RequestValidator {

    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(Object... arguments) {
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        for (Object argument : arguments) {
            if (argument != null) {
                violations.addAll(validator.validate(argument));
            }
        }
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
