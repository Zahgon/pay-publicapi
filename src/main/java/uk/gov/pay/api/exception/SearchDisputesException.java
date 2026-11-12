package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class SearchDisputesException extends ConnectorResponseErrorException {

    public SearchDisputesException(RestClientResponse response) {
        super(response);
    }

    public SearchDisputesException(Throwable cause) {
        super(cause);
    }
}
