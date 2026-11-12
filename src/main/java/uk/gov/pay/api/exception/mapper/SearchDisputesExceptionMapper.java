package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.SearchDisputesException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.Code.GET_DISPUTE_LEDGER_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_DISPUTES_NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.aRequestError;

@Singleton
public class SearchDisputesExceptionMapper extends SafeExceptionMapper<SearchDisputesException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchDisputesExceptionMapper.class);

    @Override
    protected HttpResponse<?> toResponse(SearchDisputesException exception) {
        if (exception.getErrorStatus() == NOT_FOUND.getCode()) {
            return buildResponse(exception, SEARCH_DISPUTES_NOT_FOUND, NOT_FOUND);
        }
        return buildResponse(exception, GET_DISPUTE_LEDGER_ERROR, INTERNAL_SERVER_ERROR);
    }

    private HttpResponse<?> buildResponse(SearchDisputesException exception, RequestError.Code connectorError, HttpStatus status) {
        RequestError requestError = aRequestError(connectorError);
        LOGGER.info("Ledger response was {}.\n Returning http status {} with error body {}", exception.getMessage(), status, requestError);
        return ErrorResponses.of(status, requestError);
    }
}
