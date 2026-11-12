package uk.gov.pay.api.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.api.app.config.PublicApiConfig;

import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.api.filter.LoggingFilter.MDC_REQUEST_ID_KEY;

/**
 * Sends the service's requests to connector, ledger and public auth with the JDK's HttpURLConnection, as the service
 * always has (redirects followed, a POST whose connection drops before an answer retried once). Each request carries
 * the request id of the API call it serves ({@code X-Request-Id}, empty when the call had none); an answer of any
 * status is handed back, and TLS 1.2 is used unless secure connections are disabled.
 */
@Singleton
public class RestClient {

    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);
    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String TLSV1_2 = "TLSv1.2";

    /**
     * Backend answers are read, and objects sent to a backend written, with a plain Jackson mapper: no modules, and
     * unknown properties fail unless a class ignores them.
     */
    private final ObjectMapper backendObjectMapper = new ObjectMapper();
    private final SSLSocketFactory sslSocketFactory;

    public RestClient(PublicApiConfig configuration) {
        if (configuration.getRestClientConfig().isDisabledSecureConnection()) {
            this.sslSocketFactory = null;
        } else {
            try {
                SSLContext sslContext = SSLContext.getInstance(TLSV1_2);
                sslContext.init(null, null, null);
                this.sslSocketFactory = sslContext.getSocketFactory();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IllegalStateException(String.format("Unable to find an SSL context for %s", TLSV1_2), e);
            }
        }
    }

    public RestClientResponse get(String uri, Map<String, String> headers) {
        return send("GET", uri, headers, null, null);
    }

    /**
     * Posts a JSON entity, or no entity at all when {@code jsonEntity} is null.
     */
    public RestClientResponse post(String uri, Map<String, String> headers, @Nullable String jsonEntity) {
        return send("POST", uri, headers, jsonEntity == null ? null : jsonEntity.getBytes(StandardCharsets.UTF_8),
                jsonEntity == null ? null : "application/json");
    }

    public RestClientResponse post(String uri, @Nullable String jsonEntity) {
        return post(uri, Map.of(), jsonEntity);
    }

    /**
     * Posts an object as a JSON entity written by the backend mapper as UTF-8 (characters outside the Basic
     * Multilingual Plane written as escaped surrogate pairs); a null object is an empty JSON entity.
     */
    public RestClientResponse postJson(String uri, @Nullable Object entity) {
        try {
            byte[] bytes = entity == null ? new byte[0] : backendObjectMapper.writeValueAsBytes(entity);
            return send("POST", uri, Map.of(), bytes, "application/json");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not write the entity for " + uri, e);
        }
    }

    private RestClientResponse send(String method, String uri, Map<String, String> headers, @Nullable byte[] entity,
                                    @Nullable String contentType) {
        String requestId = StringUtils.defaultString(MDC.get(MDC_REQUEST_ID_KEY));
        logger.info("[{}] - {} to {} began", requestId, method, uri, kv("method", method), kv("url", uri));
        long start = System.nanoTime();
        int status;
        byte[] answer;
        String answerType;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(uri).toURL().openConnection();
            if (sslSocketFactory != null && connection instanceof HttpsURLConnection https) {
                https.setSSLSocketFactory(sslSocketFactory);
            }
            connection.setRequestMethod(method);
            connection.setRequestProperty(HEADER_REQUEST_ID, requestId);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (contentType != null) {
                connection.setRequestProperty("Content-Type", contentType);
            }
            if (entity != null) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(entity);
                }
            }
            status = connection.getResponseCode();
            InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            answer = in == null ? new byte[0] : readAll(in);
            answerType = connection.getContentType();
        } catch (IOException | IllegalArgumentException e) {
            throw new RestClientResponse.RestClientProcessingException(e instanceof IOException io ? io : new IOException(e));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        logger.info("[{}] - {} to {} ended - total time {}ms", requestId, method, uri, elapsed,
                kv("method", method), kv("url", uri), kv("status_code", status), kv("response_time", elapsed));
        return new RestClientResponse(status, answer, answerType, backendObjectMapper);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try (in) {
            return in.readAllBytes();
        }
    }

}
