package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.BadRequestException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

@Singleton
public class BadRequestExceptionMapper extends SafeExceptionMapper<BadRequestException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BadRequestExceptionMapper.class);

    @Override
    protected HttpResponse<?> toResponse(BadRequestException exception) {

        RequestError requestError = exception.getRequestError();
        LOGGER.debug("Bad Request exception {}", requestError);

        return ErrorResponses.of(HttpStatus.BAD_REQUEST, exception.getRequestError());
    }
}
