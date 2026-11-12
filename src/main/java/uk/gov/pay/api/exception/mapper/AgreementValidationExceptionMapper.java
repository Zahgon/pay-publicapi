package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.AgreementValidationException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

@Singleton
public class AgreementValidationExceptionMapper extends SafeExceptionMapper<AgreementValidationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgreementValidationException.class);

    @Override
    protected HttpResponse<?> toResponse(AgreementValidationException exception) {
        RequestError requestError = exception.getRequestError();
        LOGGER.debug("Agreement Validation exception {}", requestError);

        return ErrorResponses.of(422, requestError);
    }

}
