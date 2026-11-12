package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class GetAgreementException extends ConnectorResponseErrorException {

    public GetAgreementException(RestClientResponse response) {
        super(response);
    }
}
