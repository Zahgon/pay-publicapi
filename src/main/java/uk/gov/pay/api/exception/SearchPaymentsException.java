package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class SearchPaymentsException extends ConnectorResponseErrorException {

    public SearchPaymentsException(RestClientResponse response) {
        super(response);
    }

    public SearchPaymentsException(Throwable cause) {
        super(cause);
    }
}
