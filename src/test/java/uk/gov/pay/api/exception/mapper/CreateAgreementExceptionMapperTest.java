package uk.gov.pay.api.exception.mapper;

import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.exception.ConnectorResponseErrorException.ConnectorErrorResponse;
import uk.gov.pay.api.exception.CreateAgreementException;
import uk.gov.pay.api.model.RequestError;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import static org.apache.hc.core5.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.hc.core5.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.pay.api.model.RequestError.aRequestError;

@ExtendWith(MockitoExtension.class)
class CreateAgreementExceptionMapperTest {

    @Mock
    private RestClientResponse mockResponse;
    private CreateAgreementExceptionMapper mapper = new CreateAgreementExceptionMapper();
    
    @Test
    void testExceptionMapping() {
        when(mockResponse.readEntity(ConnectorErrorResponse.class))
                .thenReturn(new ConnectorErrorResponse(ErrorIdentifier.GENERIC, null, null));
        HttpResponse<?> returnedResponse = mapper.handle(null, new CreateAgreementException(mockResponse));
        RequestError returnedError = (RequestError) returnedResponse.body();
        RequestError expectedError = aRequestError(RequestError.Code.CREATE_AGREEMENT_CONNECTOR_ERROR);
        assertThat(returnedResponse.code(), is(SC_INTERNAL_SERVER_ERROR));
        assertThat(returnedError.getDescription(),
                is(expectedError.getDescription()));
        assertThat(returnedError.getCode(), is(expectedError.getCode()));
    }
    
    @Test
    void testExceptionMappingForRecurringCardPaymentsNotAllowed() {
        when(mockResponse.readEntity(ConnectorErrorResponse.class))
                .thenReturn(new ConnectorErrorResponse(ErrorIdentifier.RECURRING_CARD_PAYMENTS_NOT_ALLOWED, null, null));
        HttpResponse<?> returnedResponse = mapper.handle(null, new CreateAgreementException(mockResponse));
        RequestError returnedError = (RequestError) returnedResponse.body();
        RequestError expectedError = aRequestError(RequestError.Code.RECURRING_CARD_PAYMENTS_NOT_ALLOWED_ERROR);
        assertThat(returnedResponse.code(), is(SC_UNPROCESSABLE_ENTITY));
        assertThat(returnedError.getDescription(),
                is(expectedError.getDescription()));
        assertThat(returnedError.getCode(), is(expectedError.getCode()));
    }
}
