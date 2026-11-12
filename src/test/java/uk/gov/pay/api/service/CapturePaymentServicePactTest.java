package uk.gov.pay.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.RestClientResponse;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CapturePaymentServicePactTest {

    private static final String ACCOUNT_ID = "123456";
    private static final String CHARGE_ID = "ch_e36c168c41a0";

    private CapturePaymentService capturePaymentService;

    @RegisterExtension
    public PactProviderExtension connectorRule = new PactProviderExtension("connector");

    @Mock
    private PublicApiConfig mockConfiguration;

    @BeforeEach
    public void setup() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());

        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        RestClient client = RestClients.newRestClient(false);
        capturePaymentService = new CapturePaymentService(client, connectorUriGenerator);
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-capture-payment-with-delayed-capture-true-and-awaiting-capture-request-status"})
    public void testCapturePayment() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");

        RestClientResponse capturePaymentResponse = capturePaymentService.capture(account, CHARGE_ID);

        assertThat(capturePaymentResponse.getStatus(), is(204));
    }

}
