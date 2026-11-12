package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class CaptureChargeException extends ConnectorResponseErrorException {

    public CaptureChargeException(RestClientResponse response) {
        super(response);
    }

}
