package uk.gov.pay.api.service;

import org.apache.hc.core5.http.HttpStatus;
import uk.gov.pay.api.agreement.model.AgreementCreatedResponse;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CancelAgreementException;
import uk.gov.pay.api.exception.CreateAgreementException;
import uk.gov.pay.api.exception.GetChargeException;
import uk.gov.pay.api.exception.GetEventsException;
import uk.gov.pay.api.exception.GetRefundException;
import uk.gov.pay.api.model.Charge;
import uk.gov.pay.api.model.ChargeFromResponse;
import uk.gov.pay.api.model.PaymentEvents;
import uk.gov.pay.api.model.RefundFromConnector;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;

import static org.apache.hc.core5.http.HttpStatus.SC_OK;

@Singleton
public class ConnectorService {
    private final RestClient client;
    private final ConnectorUriGenerator connectorUriGenerator;

    @Inject
    public ConnectorService(RestClient client, ConnectorUriGenerator connectorUriGenerator) {
        this.client = client;
        this.connectorUriGenerator = connectorUriGenerator;
    }

    public Charge getCharge(Account account, String paymentId) {
        RestClientResponse response = client.get(connectorUriGenerator.chargeURI(account, paymentId), Map.of());

        if (response.getStatus() == SC_OK) {
            ChargeFromResponse chargeFromResponse = response.readEntity(ChargeFromResponse.class);
            return Charge.from(chargeFromResponse);
        }

        throw new GetChargeException(response);
    }

    public PaymentEvents getChargeEvents(Account account, String paymentId) {
        RestClientResponse connectorResponse = client.get(connectorUriGenerator.chargeEventsURI(account, paymentId), Map.of("Accept", "application/json"));

        if (connectorResponse.getStatus() == SC_OK) {
            return connectorResponse.readEntity(PaymentEvents.class);
        }

        throw new GetEventsException(connectorResponse);
    }

    public RefundFromConnector getPaymentRefund(String accountId, String paymentId, String refundId) {
        RestClientResponse connectorResponse = client.get(connectorUriGenerator.refundForPaymentURI(accountId, paymentId, refundId), Map.of("Accept", "application/json"));

        if (connectorResponse.getStatus() == SC_OK) {
            return connectorResponse.readEntity(RefundFromConnector.class);
        }

        throw new GetRefundException(connectorResponse);
    }

    public AgreementCreatedResponse createAgreement(Account account, CreateAgreementRequest createAgreementRequest) {
        RestClientResponse response = client.post(connectorUriGenerator.getAgreementURI(account),
                Map.of("Accept", "application/json"), createAgreementRequest.toConnectorPayload());
        if (response.getStatus() != HttpStatus.SC_CREATED) {
            throw new CreateAgreementException(response);
        }
        return response.readEntity(AgreementCreatedResponse.class);
    }

    public void cancelAgreement(Account account, String agreementId) {
        RestClientResponse response = client.post(connectorUriGenerator.cancelAgreementURI(account, agreementId), null);
        if (response.getStatus() != HttpStatus.SC_NO_CONTENT) {
            throw new CancelAgreementException(response);
        }
    }
}
