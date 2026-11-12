package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class GetEventsException extends ConnectorResponseErrorException {

    public GetEventsException(RestClientResponse response) {
        super(response);
    }
}
