package uk.gov.pay.api.utils.pact;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The pact files ({@code src/test/resources/pacts/<name>.json}) whose interactions a test exercises. Each
 * {@link PactProviderExtension} serves the interactions of the pacts whose name contains its provider name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Pacts {
    String[] pacts();
}
