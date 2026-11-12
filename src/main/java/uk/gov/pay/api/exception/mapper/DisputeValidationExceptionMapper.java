package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.DisputesValidationException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

@Singleton
public class DisputeValidationExceptionMapper extends SafeExceptionMapper<DisputesValidationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisputeValidationExceptionMapper.class);

    @Override
    protected HttpResponse<?> toResponse(DisputesValidationException exception) {

        RequestError requestError = exception.getRequestError();
        LOGGER.debug("Dispute Validation exception {}", requestError);

        return ErrorResponses.of(422, requestError);
    }
}
