package uk.gov.pay.api.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * An answer of connector, ledger or public auth: its status and entity, read with the backend mapper.
 */
public class RestClientResponse {

    private final int status;
    private final byte[] entity;
    private final String contentType;
    private final ObjectMapper objectMapper;

    public RestClientResponse(int status, byte[] entity, ObjectMapper objectMapper) {
        this(status, entity, null, objectMapper);
    }

    public RestClientResponse(int status, byte[] entity, String contentType, ObjectMapper objectMapper) {
        this.status = status;
        this.entity = entity == null ? new byte[0] : entity;
        this.contentType = contentType;
        this.objectMapper = objectMapper;
    }

    public int getStatus() {
        return status;
    }

    public boolean hasEntity() {
        return entity.length > 0;
    }

    /**
     * Reads the entity as the given type: null when there is no entity.
     *
     * @throws RestClientProcessingException when the entity is not a JSON media type, or is not exactly one JSON value
     *                                       of that type
     */
    public <T> T readEntity(Class<T> type) {
        return read(objectMapper.readerFor(type));
    }

    public <T> T readEntity(TypeReference<T> type) {
        return read(objectMapper.readerFor(type));
    }

    private <T> T read(ObjectReader reader) {
        if (!hasEntity()) {
            return null;
        }
        if (!isJson(contentType)) {
            throw new RestClientProcessingException("No JSON reader for an entity of media type " + contentType);
        }
        try {
            return reader.with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(entity);
        } catch (IOException e) {
            throw new RestClientProcessingException(e);
        }
    }

    /**
     * A missing media type, a JSON one ({@code json} or {@code +json} subtype) or a wildcard can be read as JSON.
     */
    static boolean isJson(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return true;
        }
        String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(java.util.Locale.ROOT);
        int slash = mediaType.indexOf('/');
        if (slash < 0) {
            return false;
        }
        String subtype = mediaType.substring(slash + 1);
        return subtype.equals("json") || subtype.endsWith("+json") || subtype.equals("*");
    }

    @Override
    public String toString() {
        return "RestClientResponse{status=" + status + "}";
    }

    /**
     * A failure to reach a backend or to read its answer.
     */
    public static class RestClientProcessingException extends UncheckedIOException {

        public RestClientProcessingException(IOException cause) {
            super(cause);
        }

        public RestClientProcessingException(String message) {
            super(new IOException(message));
        }
    }
}
