package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class SearchRefundsException extends ConnectorResponseErrorException {

    public SearchRefundsException(RestClientResponse response) {
        super(response);
    }

    public SearchRefundsException(Throwable cause) {
        super(cause);
    }
}
