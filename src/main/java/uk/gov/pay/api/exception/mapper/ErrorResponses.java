package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;

/**
 * The JSON error answers the exception handlers give.
 */
public final class ErrorResponses {

    private ErrorResponses() {
    }

    public static HttpResponse<?> of(HttpStatus status, Object entity) {
        return HttpResponse.status(status).contentType(MediaType.APPLICATION_JSON_TYPE).body(entity);
    }

    public static HttpResponse<?> of(int status, Object entity) {
        return of(HttpStatus.valueOf(status), entity);
    }
}
