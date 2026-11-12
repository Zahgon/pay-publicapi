package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class SearchTransactionsException extends ConnectorResponseErrorException {

    public SearchTransactionsException(RestClientResponse response) {
        super(response);
    }

    public SearchTransactionsException(Throwable cause) {
        super(cause);
    }
}
