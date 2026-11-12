package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.ServiceUnavailableException;

import jakarta.inject.Singleton;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The answer to any exception no other handler translates: a 500 whose message carries the id the exception was
 * logged under. Public auth being unavailable is a 503.
 */
@Singleton
public class UnexpectedExceptionMapper implements ExceptionHandler<Throwable, HttpResponse<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnexpectedExceptionMapper.class);

    @Override
    public HttpResponse<?> handle(HttpRequest request, Throwable exception) {
        if (request != null) {
            ContainerErrors.errorEntity(request, HttpResponse.ok());
        }
        if (exception instanceof ServiceUnavailableException) {
            return ErrorResponses.of(503, new ErrorMessage(503, "HTTP 503 Service Unavailable"));
        }
        long id = ThreadLocalRandom.current().nextLong();
        LOGGER.error(String.format("Error handling a request: %016x", id), exception);
        return ErrorResponses.of(500, new ErrorMessage(500,
                String.format("There was an error processing your request. It has been logged (ID %016x).", id)));
    }

    /**
     * The generic error entity: the status and a message.
     */
    public record ErrorMessage(int code, String message) {
    }
}
