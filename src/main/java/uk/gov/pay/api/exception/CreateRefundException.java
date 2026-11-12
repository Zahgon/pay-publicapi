package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class CreateRefundException extends ConnectorResponseErrorException {

    public CreateRefundException(RestClientResponse response) {
        super(response);
    }
}
