package uk.gov.pay.api.exception;

/**
 * Public auth answered an API key check with an unexpected status, so the service cannot tell who is calling.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException() {
        super("HTTP 503 Service Unavailable");
    }
}
