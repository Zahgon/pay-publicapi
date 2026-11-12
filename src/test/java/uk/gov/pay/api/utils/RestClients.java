package uk.gov.pay.api.utils;

import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.app.config.RestClientConfig;

/**
 * Builds the service's backend {@link RestClient} the way the application does, for tests that call connector or
 * ledger stubs directly. Replaces {@code RestClientFactory.buildClient(...)}.
 */
public final class RestClients {

    private RestClients() {
    }

    public static RestClient newRestClient(boolean disabledSecureConnection) {
        PublicApiConfig configuration = new PublicApiConfig();
        configuration.setRestClientConfig(new RestClientConfig(disabledSecureConnection));
        return new RestClient(configuration);
    }

    public static RestClient newRestClient() {
        return newRestClient(false);
    }
}
