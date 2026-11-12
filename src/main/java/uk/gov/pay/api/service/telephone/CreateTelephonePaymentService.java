package uk.gov.pay.api.service.telephone;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.core5.http.HttpStatus;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CreateChargeException;
import uk.gov.pay.api.model.ChargeFromResponse;
import uk.gov.pay.api.model.telephone.CreateTelephonePaymentRequest;
import uk.gov.pay.api.model.telephone.TelephonePaymentResponse;
import uk.gov.pay.api.service.ConnectorUriGenerator;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class CreateTelephonePaymentService {

    private final RestClient client;
    private final ConnectorUriGenerator connectorUriGenerator;

    @Inject
    public CreateTelephonePaymentService(RestClient client, ConnectorUriGenerator connectorUriGenerator) {
        this.client = client;
        this.connectorUriGenerator = connectorUriGenerator;
    }
    
    public Pair<TelephonePaymentResponse, Integer> create(Account account, CreateTelephonePaymentRequest createTelephonePaymentRequest) {
        RestClientResponse connectorResponse = createTelephoneCharge(account, createTelephonePaymentRequest);

        if (!createdSuccessfully(connectorResponse)) {
            throw new CreateChargeException(connectorResponse);
        }

        ChargeFromResponse chargeFromResponse = connectorResponse.readEntity(ChargeFromResponse.class);
        TelephonePaymentResponse telephonePaymentResponse = TelephonePaymentResponse.from(chargeFromResponse);
        return Pair.of(telephonePaymentResponse, connectorResponse.getStatus());
    }

    private boolean createdSuccessfully(RestClientResponse connectorResponse) {
        return connectorResponse.getStatus() == HttpStatus.SC_CREATED || connectorResponse.getStatus() == HttpStatus.SC_OK;
    }

    private RestClientResponse createTelephoneCharge(Account account, CreateTelephonePaymentRequest createTelephonePaymentRequest) {
        return client.post(connectorUriGenerator.telephoneChargesURI(account),
                Map.of("Accept", "application/json"), createTelephonePaymentRequest.toConnectorPayload());
    }
}
