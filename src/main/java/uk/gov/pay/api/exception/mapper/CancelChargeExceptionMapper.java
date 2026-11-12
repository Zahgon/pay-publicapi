package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.CancelChargeException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.CONFLICT;
import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.Code.CANCEL_PAYMENT_CONNECTOR_BAD_REQUEST_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CANCEL_PAYMENT_CONNECTOR_CONFLICT_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CANCEL_PAYMENT_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CANCEL_PAYMENT_NOT_FOUND_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

@Singleton
public class CancelChargeExceptionMapper extends SafeExceptionMapper<CancelChargeException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelChargeExceptionMapper.class);

    @Override
    protected HttpResponse<?> toResponse(CancelChargeException exception) {

        int errorStatus = exception.getErrorStatus();
        RequestError requestError;
        HttpStatus status;

        if (errorStatus == NOT_FOUND.getCode()) {
            requestError = aRequestError(CANCEL_PAYMENT_NOT_FOUND_ERROR);
            status = NOT_FOUND;
        } else if (errorStatus == BAD_REQUEST.getCode()) {
            requestError = aRequestError(CANCEL_PAYMENT_CONNECTOR_BAD_REQUEST_ERROR);
            status = BAD_REQUEST;
        } else if (errorStatus == CONFLICT.getCode()) {
            requestError = aRequestError(CANCEL_PAYMENT_CONNECTOR_CONFLICT_ERROR);
            status = CONFLICT;
        } else {
            requestError = aRequestError(CANCEL_PAYMENT_CONNECTOR_ERROR);
            status = INTERNAL_SERVER_ERROR;
        }

        if (status == INTERNAL_SERVER_ERROR) {
            LOGGER.error("Connector invalid response was {}.\n Returning http status {} with error body {}", exception.getMessage(), status, requestError);
        }
        return ErrorResponses.of(status, requestError);
    }
}
