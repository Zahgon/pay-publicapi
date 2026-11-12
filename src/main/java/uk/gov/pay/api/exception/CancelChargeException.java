package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class CancelChargeException extends ConnectorResponseErrorException {

    public CancelChargeException(RestClientResponse response) {
        super(response);
    }
}
