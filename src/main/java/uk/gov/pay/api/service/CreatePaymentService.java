package uk.gov.pay.api.service;

import org.apache.hc.core5.http.HttpStatus;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CreateChargeException;
import uk.gov.pay.api.model.Charge;
import uk.gov.pay.api.model.ChargeFromResponse;
import uk.gov.pay.api.model.CreateCardPaymentRequest;
import uk.gov.pay.api.model.CreatedPaymentWithAllLinks;
import uk.gov.pay.api.model.links.PaymentWithAllLinks;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static uk.gov.pay.api.model.CreatedPaymentWithAllLinks.WhenCreated.BRAND_NEW;
import static uk.gov.pay.api.model.CreatedPaymentWithAllLinks.WhenCreated.EXISTING;

@Singleton
public class CreatePaymentService {

    private final RestClient client;
    private final PublicApiUriGenerator publicApiUriGenerator;
    private final ConnectorUriGenerator connectorUriGenerator;

    @Inject
    public CreatePaymentService(RestClient client, PublicApiUriGenerator publicApiUriGenerator, ConnectorUriGenerator connectorUriGenerator) {
        this.client = client;
        this.publicApiUriGenerator = publicApiUriGenerator;
        this.connectorUriGenerator = connectorUriGenerator;
    }

    public CreatedPaymentWithAllLinks create(Account account, CreateCardPaymentRequest createCardPaymentRequest, String idempotencyKey) {
        RestClientResponse connectorResponse = createCharge(account, createCardPaymentRequest, idempotencyKey);

        if (connectorCreatedNewPayment(connectorResponse)) {
            ChargeFromResponse chargeFromResponse = connectorResponse.readEntity(ChargeFromResponse.class);
            return CreatedPaymentWithAllLinks.of(buildResponseModel(Charge.from(chargeFromResponse)), BRAND_NEW);
        }

        if (connectorReturnedExistingPayment(connectorResponse)) {
            ChargeFromResponse chargeFromResponse = connectorResponse.readEntity(ChargeFromResponse.class);
            return CreatedPaymentWithAllLinks.of(buildResponseModel(Charge.from(chargeFromResponse)), EXISTING);
        }

        throw new CreateChargeException(connectorResponse);
    }

    private PaymentWithAllLinks buildResponseModel(Charge chargeFromResponse) {
        return PaymentWithAllLinks.getPaymentWithLinks(
                chargeFromResponse,
                publicApiUriGenerator.getPaymentURI(chargeFromResponse.getChargeId()),
                publicApiUriGenerator.getPaymentEventsURI(chargeFromResponse.getChargeId()),
                publicApiUriGenerator.getPaymentCancelURI(chargeFromResponse.getChargeId()),
                publicApiUriGenerator.getPaymentRefundsURI(chargeFromResponse.getChargeId()),
                publicApiUriGenerator.getPaymentCaptureURI(chargeFromResponse.getChargeId()),
                publicApiUriGenerator.getPaymentAuthorisationURI());
    }

    private boolean connectorCreatedNewPayment(RestClientResponse connectorResponse) {
        return connectorResponse.getStatus() == HttpStatus.SC_CREATED;
    }

    private boolean connectorReturnedExistingPayment(RestClientResponse connectorResponse) {
        return connectorResponse.getStatus() == HttpStatus.SC_OK;
    }

    private RestClientResponse createCharge(Account account, CreateCardPaymentRequest createCardPaymentRequest, String idempotencyKey) {
        Map<String, String> headers = new LinkedHashMap<>();

        Optional.ofNullable(idempotencyKey)
                .ifPresent(key -> headers.put("Idempotency-Key", idempotencyKey));

        headers.put("Accept", "application/json");
        return client.post(connectorUriGenerator.chargesURI(account), headers, createCardPaymentRequest.toConnectorPayload());
    }
}
