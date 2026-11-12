package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class AuthorisationRequestException extends ConnectorResponseErrorException{
    private RestClientResponse response;

    public AuthorisationRequestException(RestClientResponse response) {
        super(response);
        this.response = response;
    }
}
