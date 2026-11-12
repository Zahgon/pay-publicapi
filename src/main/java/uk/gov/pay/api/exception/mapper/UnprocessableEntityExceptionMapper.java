package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import uk.gov.pay.api.exception.UnprocessableEntityException;

import jakarta.inject.Singleton;

@Singleton
public class UnprocessableEntityExceptionMapper extends SafeExceptionMapper<UnprocessableEntityException> {

    @Override
    protected HttpResponse<?> toResponse(UnprocessableEntityException exception) {
        return ErrorResponses.of(422, exception.getRequestError());
    }
}
