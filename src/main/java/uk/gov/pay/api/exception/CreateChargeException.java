package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class CreateChargeException extends ConnectorResponseErrorException {

    public CreateChargeException(RestClientResponse response) {
        super(response);
    }
}
