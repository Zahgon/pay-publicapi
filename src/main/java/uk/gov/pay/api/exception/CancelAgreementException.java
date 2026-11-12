package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class CancelAgreementException extends ConnectorResponseErrorException {

    public CancelAgreementException(RestClientResponse response) {
        super(response);
    }

}
