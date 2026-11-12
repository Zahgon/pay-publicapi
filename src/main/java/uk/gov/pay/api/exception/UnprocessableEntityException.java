package uk.gov.pay.api.exception;

import uk.gov.pay.api.model.RequestError;

/**
 * A request entity that is well-formed JSON but cannot be processed, answered with 422 and the given error.
 */
public class UnprocessableEntityException extends RuntimeException {

    private final transient RequestError requestError;

    public UnprocessableEntityException(RequestError requestError) {
        super(requestError.toString());
        this.requestError = requestError;
    }

    public RequestError getRequestError() {
        return requestError;
    }
}
