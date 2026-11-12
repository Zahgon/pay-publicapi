package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class GetTransactionException extends ConnectorResponseErrorException {

    public GetTransactionException(RestClientResponse response) {
        super(response);
    }
}
