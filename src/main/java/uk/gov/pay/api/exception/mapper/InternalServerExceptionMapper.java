package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.InternalServerException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

@Singleton
public class InternalServerExceptionMapper extends SafeExceptionMapper<InternalServerException> {
    private static final Logger logger = LoggerFactory.getLogger(InternalServerExceptionMapper.class);
    @Override
    protected HttpResponse<?> toResponse(InternalServerException exception) {
        logger.error(exception.getMessage());
        RequestError error = aRequestError(CREATE_PAYMENT_CONNECTOR_ERROR);
        return ErrorResponses.of(500, error);
    }
}
