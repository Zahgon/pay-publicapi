package uk.gov.pay.api.service;

import io.micronaut.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CancelChargeException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class CancelPaymentService {

    private final RestClient client;
    private final ConnectorUriGenerator connectorUriGenerator;

    @Inject
    public CancelPaymentService(RestClient client, ConnectorUriGenerator connectorUriGenerator) {
        this.client = client;
        this.connectorUriGenerator = connectorUriGenerator;
    }

    public HttpResponse<?> cancel(Account account, String chargeId) {
        RestClientResponse connectorResponse = client.post(connectorUriGenerator.cancelURI(account, chargeId), null);

        if (connectorResponse.getStatus() == HttpStatus.SC_NO_CONTENT) {
            return HttpResponse.noContent();
        }

        throw new CancelChargeException(connectorResponse);
    }

}
