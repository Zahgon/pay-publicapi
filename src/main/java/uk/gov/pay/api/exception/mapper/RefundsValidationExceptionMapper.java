package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.RefundsValidationException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

@Singleton
public class RefundsValidationExceptionMapper extends SafeExceptionMapper<RefundsValidationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefundsValidationExceptionMapper.class);

    @Override
    protected HttpResponse<?> toResponse(RefundsValidationException exception) {

        RequestError requestError = exception.getRequestError();
        LOGGER.debug("Refunds Validation exception {}", requestError);

        return ErrorResponses.of(422, requestError);
    }
}
