package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.SearchAgreementsException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_AGREEMENTS_LEDGER_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_AGREEMENTS_NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.aRequestError;

@Singleton
public class SearchAgreementsExceptionMapper extends SafeExceptionMapper<SearchAgreementsException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchAgreementsExceptionMapper.class);

    @Override
    protected HttpResponse<?> toResponse(SearchAgreementsException exception) {
        if (exception.getErrorStatus() == NOT_FOUND.getCode()) {
            return ErrorResponses.of(NOT_FOUND, aRequestError(SEARCH_AGREEMENTS_NOT_FOUND));
        } else {
            RequestError requestError = aRequestError(SEARCH_AGREEMENTS_LEDGER_ERROR);
            final HttpStatus status = INTERNAL_SERVER_ERROR;
            LOGGER.error("Ledger response was {}.\n Returning http status {} with error body {}", exception.getMessage(), status, requestError);
            return ErrorResponses.of(status, requestError);
        }
    }

}
