package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.GetEventsException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.Code.GET_PAYMENT_EVENTS_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.GET_PAYMENT_EVENTS_NOT_FOUND_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

@Singleton
public class GetEventsExceptionMapper extends SafeExceptionMapper<GetEventsException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetEventsExceptionMapper.class);

    @Override
    protected HttpResponse<?> toResponse(GetEventsException exception) {

        RequestError requestError;
        HttpStatus status;

        if (exception.getErrorStatus() == NOT_FOUND.getCode()) {
            requestError = aRequestError(GET_PAYMENT_EVENTS_NOT_FOUND_ERROR);
            status = NOT_FOUND;
        } else {
            requestError = aRequestError(GET_PAYMENT_EVENTS_CONNECTOR_ERROR);
            status = INTERNAL_SERVER_ERROR;
            LOGGER.error("Connector invalid response was {}.\n Returning http status {} with error body {}", exception.getMessage(), status, requestError);
        }

        return ErrorResponses.of(status, requestError);
    }
}
