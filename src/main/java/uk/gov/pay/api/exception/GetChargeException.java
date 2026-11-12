package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class GetChargeException extends ConnectorResponseErrorException {

    public GetChargeException(RestClientResponse response) {
        super(response);
    }
}
