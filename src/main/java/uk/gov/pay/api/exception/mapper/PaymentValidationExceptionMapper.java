package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.PaymentValidationException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

@Singleton
public class PaymentValidationExceptionMapper extends SafeExceptionMapper<PaymentValidationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentValidationExceptionMapper.class);

    @Override
    protected HttpResponse<?> toResponse(PaymentValidationException exception) {

        RequestError requestError = exception.getRequestError();
        LOGGER.debug("Payment Validation exception {}", requestError);

        return ErrorResponses.of(422, requestError);
    }
}
