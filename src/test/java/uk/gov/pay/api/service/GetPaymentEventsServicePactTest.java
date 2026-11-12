package uk.gov.pay.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.pay.api.app.RestClient;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.PaymentEventsResponse;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.utils.RestClients;
import uk.gov.pay.api.utils.pact.PactProviderExtension;
import uk.gov.pay.api.utils.pact.Pacts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GetPaymentEventsServicePactTest {

    @RegisterExtension
    public PactProviderExtension connectorRule = new PactProviderExtension("connector");

    @RegisterExtension
    public PactProviderExtension ledgerRule = new PactProviderExtension("ledger");

    @Mock
    private PublicApiConfig mockConfiguration;

    private GetPaymentEventsService getPaymentEventsService;
    private static final String ACCOUNT_ID = "42";

    @BeforeEach
    public void setUp() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());
        when(mockConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        PublicApiUriGenerator publicApiUriGenerator = new PublicApiUriGenerator(mockConfiguration);
        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);
        RestClient client = RestClients.newRestClient(false);
        LedgerService ledgerService = new LedgerService(client, ledgerUriGenerator);
        ConnectorService connectorService = new ConnectorService(client, connectorUriGenerator);

        getPaymentEventsService = new GetPaymentEventsService(publicApiUriGenerator, connectorService, ledgerService);
    }

    @Test
    @Pacts(pacts = {"publicapi-connector-get-payment-events"})
    public void shouldReturnPaymentEventsWhenCallingConnector() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");
        PaymentEventsResponse paymentEventsResponse = getPaymentEventsService.getPaymentEventsFromConnector(account, "abc123");
        assertThat(paymentEventsResponse.getPaymentId(), is("abc123"));
        assertThat(paymentEventsResponse.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/abc123/events"));
        assertThat(paymentEventsResponse.getEvents().size(), is(2));
        assertThat(paymentEventsResponse.getEvents().get(0).getPaymentId(), is("abc123"));
        assertThat(paymentEventsResponse.getEvents().get(0).getPaymentLink().getPaymentLink().getHref(), is("http://publicapi.test.localhost/v1/payments/abc123"));
        assertThat(paymentEventsResponse.getEvents().get(0).getState().getStatus(), is("created"));
        assertThat(paymentEventsResponse.getEvents().get(0).getState().isFinished(), is(false));
        assertThat(paymentEventsResponse.getEvents().get(0).getUpdated(), is("2019-08-06T10:34:43.487Z"));
        assertThat(paymentEventsResponse.getEvents().get(1).getPaymentId(), is("abc123"));
        assertThat(paymentEventsResponse.getEvents().get(1).getPaymentLink().getPaymentLink().getHref(), is("http://publicapi.test.localhost/v1/payments/abc123"));
        assertThat(paymentEventsResponse.getEvents().get(1).getState().getStatus(), is("failed"));
        assertThat(paymentEventsResponse.getEvents().get(1).getState().isFinished(), is(true));
        assertThat(paymentEventsResponse.getEvents().get(1).getState().getCode(), is("P0010"));
        assertThat(paymentEventsResponse.getEvents().get(1).getState().getMessage(), is("Payment method rejected"));
        assertThat(paymentEventsResponse.getEvents().get(1).getUpdated(), is("2019-08-06T10:34:48.487Z"));
    }

    @Test
    @Pacts(pacts = {"publicapi-ledger-get-payment-events"})
    public void shouldReturnPaymentEventsWhenCallingLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");
        PaymentEventsResponse paymentEventsResponse = getPaymentEventsService.getPaymentEventsFromLedger(account, "abc123");
        assertThat(paymentEventsResponse.getPaymentId(), is("abc123"));
        assertThat(paymentEventsResponse.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/abc123/events"));
        assertThat(paymentEventsResponse.getEvents().size(), is(2));
        assertThat(paymentEventsResponse.getEvents().get(0).getPaymentId(), is("abc123"));
        assertThat(paymentEventsResponse.getEvents().get(0).getPaymentLink().getPaymentLink().getHref(), is("http://publicapi.test.localhost/v1/payments/abc123"));
        assertThat(paymentEventsResponse.getEvents().get(0).getState().getStatus(), is("created"));
        assertThat(paymentEventsResponse.getEvents().get(0).getState().isFinished(), is(false));
        assertThat(paymentEventsResponse.getEvents().get(0).getUpdated(), is("2019-08-06T10:34:43.487123Z"));
        assertThat(paymentEventsResponse.getEvents().get(1).getPaymentId(), is("abc123"));
        assertThat(paymentEventsResponse.getEvents().get(1).getPaymentLink().getPaymentLink().getHref(), is("http://publicapi.test.localhost/v1/payments/abc123"));
        assertThat(paymentEventsResponse.getEvents().get(1).getState().getStatus(), is("failed"));
        assertThat(paymentEventsResponse.getEvents().get(1).getState().isFinished(), is(true));
        assertThat(paymentEventsResponse.getEvents().get(1).getState().getCode(), is("P0010"));
        assertThat(paymentEventsResponse.getEvents().get(1).getState().getMessage(), is("Payment method rejected"));
        assertThat(paymentEventsResponse.getEvents().get(1).getUpdated(), is("2019-08-06T10:34:48.123456Z"));
    }
}
