package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class GetRefundException extends ConnectorResponseErrorException {
    public GetRefundException(RestClientResponse response) {
        super(response);
    }

    public GetRefundException(GetTransactionException exception) {
        super(exception);
    }
}
