package uk.gov.pay.api.validation;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;

import jakarta.inject.Singleton;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * The Bean Validation (Hibernate Validator) validator for request entities and headers, with its standard messages.
 * Its factory lives as long as the application and is closed with it.
 */
@Factory
public class ValidatorFactory {

    @Singleton
    @Bean(preDestroy = "close")
    public jakarta.validation.ValidatorFactory validatorFactory() {
        return Validation.byDefaultProvider().configure().buildValidatorFactory();
    }

    @Singleton
    public Validator validator(jakarta.validation.ValidatorFactory validatorFactory) {
        return validatorFactory.getValidator();
    }
}
