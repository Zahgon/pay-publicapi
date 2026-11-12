package uk.gov.pay.api.exception;

import uk.gov.pay.api.app.RestClientResponse;

public class SearchAgreementsException extends ConnectorResponseErrorException {

    public SearchAgreementsException(RestClientResponse response) {
        super(response);
    }

    public SearchAgreementsException(Throwable cause) {
        super(cause);
    }
}
