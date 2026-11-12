package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.servlet.http.ServletHttpRequest;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Hands an answer to the servlet container's own error page: when translating an exception fails, the service has
 * always answered with the container's 500 page rather than an entity of its own.
 */
public final class ContainerErrors {

    public static final String CONTAINER_ERROR_ATTRIBUTE = "uk.gov.pay.api.containerError";

    /**
     * Marks an answer as an error entity: its media type is always plain {@code application/json}, unlike a resource's
     * answer, which takes the parameters of the client's {@code Accept} header.
     */
    public static final String ERROR_ENTITY_ATTRIBUTE = "uk.gov.pay.api.errorEntity";

    private ContainerErrors() {
    }

    public static HttpResponse<?> containerError(HttpRequest<?> request, int status) {
        mark(request, CONTAINER_ERROR_ATTRIBUTE, status);
        return HttpResponse.status(HttpStatus.valueOf(status));
    }

    public static HttpResponse<?> errorEntity(HttpRequest<?> request, HttpResponse<?> response) {
        mark(request, ERROR_ENTITY_ATTRIBUTE, Boolean.TRUE);
        return response;
    }

    private static void mark(HttpRequest<?> request, String attribute, Object value) {
        if (request instanceof ServletHttpRequest<?, ?> servletRequest
                && servletRequest.getNativeRequest() instanceof HttpServletRequest nativeRequest) {
            nativeRequest.setAttribute(attribute, value);
        }
    }
}
