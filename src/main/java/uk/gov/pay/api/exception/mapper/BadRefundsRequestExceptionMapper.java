package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.BadRefundsRequestException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

@Singleton
public class BadRefundsRequestExceptionMapper extends SafeExceptionMapper<BadRefundsRequestException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BadRefundsRequestExceptionMapper.class);

    @Override
    protected HttpResponse<?> toResponse(BadRefundsRequestException exception) {

        RequestError requestError = exception.getRequestError();
        LOGGER.debug("Bad Refunds Request exception {}", requestError);

        return ErrorResponses.of(HttpStatus.BAD_REQUEST, exception.getRequestError());
    }
}
