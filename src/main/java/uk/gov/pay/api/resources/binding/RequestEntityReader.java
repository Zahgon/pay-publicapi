package uk.gov.pay.api.resources.binding;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.servlet.http.ServletHttpRequest;

import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Reads a request's JSON entity with the API's mapper, from the raw bytes: the encoding is detected from the
 * content (UTF-8, UTF-16 or UTF-32), whatever charset the {@code Content-Type} names. Malformed or mistyped JSON
 * raises the mapper's exception, which the exception handlers translate.
 */
@Singleton
public class RequestEntityReader {

    private final ObjectMapper objectMapper;

    public RequestEntityReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @throws EmptyEntityException when the request has no entity
     * @throws com.fasterxml.jackson.core.JsonProcessingException when the entity is not one JSON value of the type
     */
    public <T> T read(HttpRequest<?> request, Class<T> type) throws IOException {
        byte[] entity = entityOf(request);
        if (!hasJsonContent(entity)) {
            throw new EmptyEntityException();
        }
        return objectMapper.readerFor(type)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readValue(entity);
    }

    /**
     * Like {@link #read(HttpRequest, Class)}, but a request without JSON content (no entity, or only whitespace) reads
     * as null.
     */
    public <T> T readOptional(HttpRequest<?> request, Class<T> type) throws IOException {
        byte[] entity = entityOf(request);
        if (!hasJsonContent(entity)) {
            return null;
        }
        return objectMapper.readerFor(type)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readValue(entity);
    }

    private boolean hasJsonContent(byte[] entity) throws IOException {
        if (entity.length == 0) {
            return false;
        }
        try (JsonParser parser = objectMapper.getFactory().createParser(entity)) {
            return parser.nextToken() != null;
        } catch (JsonProcessingException e) {
            return true;
        }
    }

    /**
     * A request that should carry a JSON entity carries none.
     */
    public static class EmptyEntityException extends RuntimeException {
        public EmptyEntityException() {
            super("The request has no entity");
        }
    }

    private static byte[] entityOf(HttpRequest<?> request) {
        if (request instanceof ServletHttpRequest<?, ?> servletRequest) {
            try {
                return servletRequest.getInputStream().readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return request.getBody(byte[].class).orElse(new byte[0]);
    }
}
