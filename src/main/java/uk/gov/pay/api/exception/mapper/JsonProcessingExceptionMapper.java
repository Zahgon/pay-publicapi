package uk.gov.pay.api.exception.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.server.exceptions.JacksonExceptionHandler;
import org.apache.hc.core5.http.HttpStatus;
import uk.gov.pay.api.exception.AgreementValidationException;
import uk.gov.pay.api.exception.PaymentValidationException;
import uk.gov.pay.api.model.RequestError;

import jakarta.inject.Singleton;

import static uk.gov.pay.api.model.RequestError.aRequestError;

@Singleton
@Replaces(JacksonExceptionHandler.class)
public class JsonProcessingExceptionMapper extends SafeExceptionMapper<JsonProcessingException> {

    private final UnexpectedExceptionMapper unexpectedExceptionMapper;

    public JsonProcessingExceptionMapper(UnexpectedExceptionMapper unexpectedExceptionMapper) {
        this.unexpectedExceptionMapper = unexpectedExceptionMapper;
    }

    @Override
    protected HttpResponse<?> toResponse(JsonProcessingException exception) {
        if (exception.getCause() instanceof PaymentValidationException) {
            RequestError requestError = ((PaymentValidationException) exception.getCause()).getRequestError();
            return ErrorResponses.of(HttpStatus.SC_UNPROCESSABLE_ENTITY, requestError);
        } else if (exception.getCause() instanceof AgreementValidationException) {
            RequestError requestError = ((AgreementValidationException) exception.getCause()).getRequestError();
            return ErrorResponses.of(HttpStatus.SC_UNPROCESSABLE_ENTITY, requestError);
        } else if (exception instanceof MismatchedInputException mismatchedInputException) {
            String typeName = isNumeric(mismatchedInputException.getTargetType()) ?
                    "numeric" :
                    mismatchedInputException.getTargetType().getSimpleName();
            String message = String.format("Must be a valid %s format", typeName);
            var requestError = aRequestError(mismatchedInputException.getPath().get(0).getFieldName(),
                    RequestError.Code.CREATE_PAYMENT_VALIDATION_ERROR,
                    message);
            return ErrorResponses.of(HttpStatus.SC_UNPROCESSABLE_ENTITY, requestError);
        } else {
            return unexpectedExceptionMapper.handle(null, exception);
        }
    }

    private boolean isNumeric(Class<?> type) {
        return type == int.class || type == long.class || type == double.class || Number.class.isAssignableFrom(type);
    }
}
