package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.SearchRefundsException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_REFUNDS_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_REFUNDS_NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.aRequestError;

@Singleton
public class SearchRefundsExceptionMapper extends SafeExceptionMapper<SearchRefundsException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchRefundsExceptionMapper.class);

    @Override
    protected HttpResponse<?> toResponse(SearchRefundsException exception) {
        if (exception.getErrorStatus() == NOT_FOUND.getCode()) {
            return buildResponse(exception, SEARCH_REFUNDS_NOT_FOUND, NOT_FOUND);
        }
        return buildResponse(exception, SEARCH_REFUNDS_CONNECTOR_ERROR, INTERNAL_SERVER_ERROR);
    }

    private HttpResponse<?> buildResponse(SearchRefundsException exception, RequestError.Code connectorError, HttpStatus status) {
        RequestError refundError = aRequestError(connectorError);
        LOGGER.info("Connector response was {}.\n Returning http status {} with error body {}", exception.getMessage(), status, refundError);
        return ErrorResponses.of(status, refundError);
    }
}
