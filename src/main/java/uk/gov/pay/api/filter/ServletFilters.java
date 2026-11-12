package uk.gov.pay.api.filter;

import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.Order;
import io.micronaut.servlet.api.annotation.ServletFilterBean;
import io.micronaut.web.router.Router;
import uk.gov.pay.api.app.config.PublicApiConfig;

import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.servlet.Filter;

/**
 * The servlet filters in front of the API, in the order they run: the logging context is cleared and the call
 * logged, the API key's checksum is checked for {@code /v1} paths, and the request is matched against the resources.
 */
@Factory
public class ServletFilters {

    @Order(1)
    @Named("ClearMdcValuesFilter")
    @ServletFilterBean(filterName = "ClearMdcValuesFilter", value = "/v1/*")
    Filter clearMdcValuesFilter() {
        return new ClearMdcValuesFilter();
    }

    @Order(2)
    @Named("LoggingFilter")
    @ServletFilterBean(filterName = "LoggingFilter", value = "/v1/*")
    Filter loggingFilter() {
        return new LoggingFilter();
    }

    @Order(3)
    @Named("AuthorizationValidationFilter")
    @ServletFilterBean(filterName = "AuthorizationValidationFilter", value = "/v1/*")
    Filter authorizationValidationFilter(PublicApiConfig configuration) {
        return new AuthorizationValidationFilter(configuration);
    }

    @Order(4)
    @Named("ResourceMatchingFilter")
    @ServletFilterBean(filterName = "ResourceMatchingFilter", value = "/*")
    Filter resourceMatchingFilter(Provider<Router> router) {
        return new ResourceMatchingFilter(router);
    }
}
