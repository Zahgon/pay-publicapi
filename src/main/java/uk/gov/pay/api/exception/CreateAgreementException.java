package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class CreateAgreementException extends ConnectorResponseErrorException {
    public CreateAgreementException(RestClientResponse response) {
        super(response);
    }
}
