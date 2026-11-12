package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An exception handler whose own failure (for example a backend error entity missing a field the translation needs)
 * ends in the servlet container's 500 page.
 */
public abstract class SafeExceptionMapper<E extends Throwable> implements ExceptionHandler<E, HttpResponse<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SafeExceptionMapper.class);

    @Override
    public final HttpResponse<?> handle(HttpRequest request, E exception) {
        try {
            return ContainerErrors.errorEntity(request, toResponse(exception));
        } catch (RuntimeException failure) {
            LOGGER.error("Could not translate {} into an answer", exception.getClass().getSimpleName(), failure);
            return ContainerErrors.containerError(request, 500);
        }
    }

    protected abstract HttpResponse<?> toResponse(E exception);
}
