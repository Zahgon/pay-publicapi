package uk.gov.pay.api.resources.binding;

import io.micronaut.core.bind.annotation.Bindable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the first value of the named query parameter, decoded as described by
 * {@link uk.gov.pay.api.utils.QueryParameters}, or null when the request does not carry it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@Bindable
public @interface QueryParameter {

    String value();
}
