package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class GetRefundsException extends ConnectorResponseErrorException {
    public GetRefundsException(RestClientResponse response) {
        super(response);
    }
}
