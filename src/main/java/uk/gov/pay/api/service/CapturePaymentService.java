package uk.gov.pay.api.service;

import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.auth.Account;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class CapturePaymentService {

    private final RestClient client;
    private final ConnectorUriGenerator connectorUriGenerator;

    @Inject
    public CapturePaymentService(RestClient client, ConnectorUriGenerator connectorUriGenerator) {
        this.client = client;
        this.connectorUriGenerator = connectorUriGenerator;
    }

    public RestClientResponse capture(Account account, String chargeId) {
        return client.post(connectorUriGenerator.captureURI(account, chargeId), "{}");
    }

}
